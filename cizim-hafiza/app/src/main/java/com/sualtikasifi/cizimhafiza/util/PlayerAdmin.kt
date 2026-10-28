package com.sualtikasifi.cizimhafiza.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.domain.model.Moderation
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Everyone who reported the same player, folded into one row for the panel. */
data class PlayerReportGroup(
    val uid: String,
    val nickname: String,
    val reasons: Map<String, Int>,
    val contexts: List<String>,
    val latestMillis: Long,
    val reportIds: List<String>
) {
    val count: Int get() = reportIds.size
}

/** One account as the player-lookup shows it. */
data class PlayerInfo(
    val uid: String,
    val nickname: String,
    val level: Long,
    val periodXp: Long,
    val friendCode: String,
    val strikes: Int,
    /** The latest lock still running, or 0. */
    val lockedUntilMillis: Long,
    val reports: Int
)

/** The developer panel's hand on a single player: reports, lookup, name reset, lock, compensation — each one audited. */
@Singleton
class PlayerAdmin @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun reports(): Result<List<PlayerReportGroup>> = runCatching {
        firestore.collection("playerReports").limit(MAX_REPORTS).get().await().documents
            .groupBy { it.getString("reportedUid").orEmpty() }
            .filterKeys { it.isNotEmpty() }
            .map { (uid, docs) ->
                PlayerReportGroup(
                    uid = uid,
                    nickname = docs.firstNotNullOfOrNull { it.getString("nickname")?.takeIf(String::isNotBlank) }.orEmpty(),
                    reasons = docs.groupingBy { it.getString("reason").orEmpty() }.eachCount(),
                    contexts = docs.mapNotNull { it.getString("context")?.takeIf(String::isNotBlank) }.distinct(),
                    latestMillis = docs.maxOfOrNull { it.getTimestamp("createdAt")?.toDate()?.time ?: 0L } ?: 0L,
                    reportIds = docs.map { it.id }
                )
            }
            .sortedWith(compareByDescending<PlayerReportGroup> { it.count }.thenByDescending { it.latestMillis })
    }

    suspend fun dismissReports(group: PlayerReportGroup): Result<Unit> = runCatching {
        val batch = firestore.batch()
        group.reportIds.forEach { batch.delete(firestore.collection("playerReports").document(it)) }
        batch.commit().await()
        audit("report-dismiss", group.uid, "${group.count} rapor kapatıldı")
    }

    /** Puts a neutral name on the account, so a reported name stops showing in the league and in rooms. */
    suspend fun resetName(uid: String, oldName: String): Result<String> = runCatching {
        val fresh = "Oyuncu" + (1000..9999).random()
        firestore.collection("users").document(uid).update(
            mapOf("nickname" to fresh, "username" to fresh, "nameResetAt" to System.currentTimeMillis())
        ).await()
        audit("name-reset", uid, "\"$oldName\" -> \"$fresh\"")
        fresh
    }

    /** Locks the player out of the online modes one rung up the ladder ([Moderation.lockoutMillisFor]); no XP is taken. */
    suspend fun lock(uid: String): Result<Long> = runCatching {
        val strikes = firestore.collection("moderationStrikes").document(uid)
        val previous = (strikes.get().await().getLong("count") ?: 0L).toInt()
        val strike = previous + 1
        val now = System.currentTimeMillis()
        val until = now + Moderation.lockoutMillisFor(strike)
        val batch = firestore.batch()
        batch.set(
            firestore.collection("penalties").document(),
            mapOf(
                "uid" to uid, "xpRevoked" to 0L, "goldRevoked" to 0L, "strike" to strike.toLong(),
                "lockedUntil" to until, "appliedAt" to 0L, "createdAt" to now
            )
        )
        batch.set(strikes, mapOf("count" to strike.toLong(), "updatedAt" to now))
        batch.commit().await()
        audit("lock", uid, "ceza $strike, ${(until - now) / 3_600_000} saat")
        until
    }

    /** Lifts every lock still running on the account and takes one strike back for each. */
    suspend fun unlock(uid: String): Result<Int> = runCatching {
        val now = System.currentTimeMillis()
        val running = firestore.collection("penalties").whereEqualTo("uid", uid).get().await().documents
            .filter { (it.getLong("lockedUntil") ?: 0L) > now }
        val strikes = firestore.collection("moderationStrikes").document(uid)
        val previous = (strikes.get().await().getLong("count") ?: 0L).toInt()
        val batch = firestore.batch()
        running.forEach { doc ->
            val update = mutableMapOf<String, Any>("lockedUntil" to 0L, "canceledAt" to now)
            if ((doc.getLong("appliedAt") ?: 0L) == 0L) {
                update["xpRevoked"] = 0L
                update["goldRevoked"] = 0L
                update["appliedAt"] = now
            }
            batch.update(doc.reference, update)
        }
        if (running.isNotEmpty()) {
            batch.set(strikes, mapOf("count" to (previous - running.size).coerceAtLeast(0).toLong(), "updatedAt" to now))
            batch.commit().await()
            audit("unlock", uid, "${running.size} kilit kaldırıldı")
        }
        running.size
    }

    /** Asks the cron script to drop gold and/or XP into the player's pending rewards (their app claims it on start). */
    suspend fun grant(uid: String, gold: Int, xp: Int, note: String): Result<Unit> = runCatching {
        require(gold in 0..MAX_GRANT && xp in 0..MAX_GRANT && gold + xp > 0) { "Miktar geçersiz" }
        firestore.collection("adminCommands").add(
            mapOf(
                "type" to "grant", "uid" to uid, "gold" to gold.toLong(), "xp" to xp.toLong(),
                "note" to note.take(60), "by" to (auth.currentUser?.email ?: ""), "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        audit("grant", uid, "altın $gold, XP $xp: ${note.take(60)}")
    }

    /** Finds players by name, friend code or uid. Several come back when a name is shared. */
    suspend fun find(query: String): Result<List<PlayerInfo>> = runCatching {
        val q = query.trim()
        require(q.isNotEmpty()) { "Bir ad, arkadaş kodu ya da kullanıcı kimliği yaz." }
        val uids = linkedSetOf<String>()
        if (q.length >= 20 && q.none(Char::isWhitespace)) uids += q
        if (q.length == 6 && q.all(Char::isDigit)) {
            firestore.collection("friendCodes").document(q).get().await().getString("uid")?.let(uids::add)
        }
        firestore.collection("usernames").document(UsernameRepository.keyOf(q)).get().await().getString("uid")?.let(uids::add)
        firestore.collection("users").whereEqualTo("nickname", q).limit(5).get().await().documents.forEach { uids += it.id }
        uids.take(5).mapNotNull { info(it) }
    }

    private suspend fun info(uid: String): PlayerInfo? {
        val user: DocumentSnapshot = firestore.collection("users").document(uid).get().await()
        if (!user.exists()) return null
        val now = System.currentTimeMillis()
        val strikes = runCatching { firestore.collection("moderationStrikes").document(uid).get().await().getLong("count") }.getOrNull() ?: 0L
        val locked = runCatching {
            firestore.collection("penalties").whereEqualTo("uid", uid).get().await().documents
                .maxOfOrNull { it.getLong("lockedUntil") ?: 0L } ?: 0L
        }.getOrDefault(0L).takeIf { it > now } ?: 0L
        val reports = runCatching {
            firestore.collection("playerReports").whereEqualTo("reportedUid", uid).get().await().size()
        }.getOrDefault(0)
        return PlayerInfo(
            uid = uid,
            nickname = user.getString("nickname").orEmpty(),
            level = user.getLong("level") ?: 0L,
            periodXp = user.getLong("periodXp") ?: 0L,
            friendCode = user.getString("friendCode").orEmpty(),
            strikes = strikes.toInt(),
            lockedUntilMillis = locked,
            reports = reports
        )
    }

    /** One line in the audit log; failing to write it never fails the action it describes. */
    suspend fun audit(action: String, target: String, detail: String) {
        runCatching {
            firestore.collection("adminAudit").add(
                mapOf(
                    "action" to action, "target" to target, "detail" to detail.take(200),
                    "by" to (auth.currentUser?.email ?: ""), "at" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    private companion object {
        const val MAX_REPORTS = 400L
        const val MAX_GRANT = 100_000
    }
}
