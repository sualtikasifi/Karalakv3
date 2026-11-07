package com.sualtikasifi.cizimhafiza.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.sualtikasifi.cizimhafiza.domain.model.Moderation
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

/** One account as the player-lookup lists it: what its public profile holds. */
data class PlayerInfo(
    val uid: String,
    val nickname: String,
    val level: Long,
    val periodXp: Long,
    val totalXp: Long,
    val friendCode: String
)

/** One penalty on an account's record, newest first in [PlayerDetails.penalties]. */
data class PenaltyLine(
    val strike: Int,
    val xpRevoked: Long,
    val goldRevoked: Long,
    val lockedUntilMillis: Long,
    val createdAtMillis: Long,
    val canceled: Boolean
)

/** What the lookup shows once one account is opened: its record with the moderation, its cloud save and its rounds. Every part may be missing (null) when it could not be read. */
data class PlayerDetails(
    val strikes: Int = 0,
    /** The latest lock still running, or 0. */
    val lockedUntilMillis: Long = 0L,
    val reports: Int = 0,
    val penalties: List<PenaltyLine> = emptyList(),
    /** From the cloud save (users/{uid}/backup/state), which the reviewer may read: the balance and when it was last saved. */
    val goldBalance: Long? = null,
    val lifetimeXp: Long? = null,
    val lastSavedMillis: Long? = null,
    /** Rounds of this player that are in the live pool and waiting in the queue. */
    val poolRuns: Long? = null,
    val queueRuns: Long? = null
)

/** A coupon this account redeemed. */
data class CouponUse(val code: String, val redeemedAtMillis: Long)

/** The spellings and bounds a player is looked for under, apart from Firestore so they can be tested. */
object PlayerSearch {
    /** The most accounts a search lists. */
    const val MAX_RESULTS = 10
    /** A prefix search starts from this many characters. */
    const val MIN_PREFIX = 2

    fun isFriendCode(q: String) = q.length == 6 && q.all(Char::isDigit)
    fun looksLikeUid(q: String) = q.length >= 20 && q.none(Char::isWhitespace)

    /** The end of a prefix range: every string that starts with [prefix] sorts before this one. */
    fun upperBound(prefix: String) = prefix + "\uf8ff"

    /**
     * The nicknames are stored as typed, so a prefix search is case sensitive: it is tried as typed and with the first letter
     * in capitals (the usual way a name is written), Turkish rules for the i.
     */
    fun nicknamePrefixes(q: String): List<String> {
        val tr = java.util.Locale("tr")
        val capital = q.replaceFirstChar { it.titlecase(tr) }
        return listOf(q, capital, q.lowercase(tr)).filter { it.length >= MIN_PREFIX }.distinct()
    }
}

/** The developer panel's hand on a single player: reports, lookup, name reset, lock, compensation — each one audited. */
@Singleton
class PlayerAdmin @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun reports(): Result<List<PlayerReportGroup>> = runCatching {
        // the newest MAX_REPORTS: an unordered limit would leave out whichever ones happened to come last
        firestore.collection("playerReports").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(MAX_REPORTS).get().await().documents
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
        audited("report-dismiss", group.uid, "${group.count} rapor kapatıldı") { batch ->
            group.reportIds.forEach { batch.delete(firestore.collection("playerReports").document(it)) }
        }
    }

    /**
     * One change and its audit-log line as a single write: both land or neither does. The panels use this for everything
     * they change, so an action nobody recorded can no longer happen (a line written afterwards, on its own, could fail alone).
     */
    suspend fun audited(action: String, target: String, detail: String, writes: (WriteBatch) -> Unit) {
        AdminAudit.commit(firestore, auth.currentUser?.email, action, target, detail, writes)
    }

    /** Puts a neutral name on the account, so a reported name stops showing in the league and in rooms. */
    suspend fun resetName(uid: String, oldName: String): Result<String> = runCatching {
        val fresh = "Oyuncu" + (1000..9999).random()
        audited("name-reset", uid, "\"$oldName\" -> \"$fresh\"") { batch ->
            batch.update(
                firestore.collection("users").document(uid),
                mapOf("nickname" to fresh, "username" to fresh, "nameResetAt" to System.currentTimeMillis())
            )
        }
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
        AdminAudit.add(firestore, batch, auth.currentUser?.email, "lock", uid, "ceza $strike, ${(until - now) / 3_600_000} saat")
        batch.commit().await()
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
            AdminAudit.add(firestore, batch, auth.currentUser?.email, "unlock", uid, "${running.size} kilit kaldırıldı")
            batch.commit().await()
        }
        running.size
    }

    /** Asks the cron script to drop gold and/or XP into the player's pending rewards (their app claims it on start). */
    suspend fun grant(uid: String, gold: Int, xp: Int, note: String): Result<Unit> = runCatching {
        require(gold in 0..MAX_GRANT && xp in 0..MAX_GRANT && gold + xp > 0) { "Miktar geçersiz" }
        audited("grant", uid, "altın $gold, XP $xp: ${note.take(60)}") { batch ->
            batch.set(
                firestore.collection("adminCommands").document(),
                mapOf(
                    "type" to "grant", "uid" to uid, "gold" to gold.toLong(), "xp" to xp.toLong(),
                    "note" to note.take(60), "by" to (auth.currentUser?.email ?: ""), "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
    }

    /**
     * Finds players by uid, friend code or name. A name matches from its beginning, whatever its case or accents (through the
     * usernames table, whose keys are folded), and, when that finds few, as typed. Up to [PlayerSearch.MAX_RESULTS] come back,
     * each from its one profile document; [details] opens one.
     */
    suspend fun find(query: String): Result<List<PlayerInfo>> = runCatching {
        val q = query.trim()
        require(q.isNotEmpty()) { "Bir ad, arkadaş kodu ya da kullanıcı kimliği yaz." }
        val uids = linkedSetOf<String>()
        if (PlayerSearch.looksLikeUid(q)) uids += q
        if (PlayerSearch.isFriendCode(q)) {
            firestore.collection("friendCodes").document(q).get().await().getString("uid")?.let(uids::add)
        }
        val key = UsernameRepository.keyOf(q)
        if (key.isNotEmpty()) {
            firestore.collection("usernames").document(key).get().await().getString("uid")?.let(uids::add)
            if (key.length >= PlayerSearch.MIN_PREFIX) {
                firestore.collection("usernames")
                    .whereGreaterThanOrEqualTo(FieldPath.documentId(), key)
                    .whereLessThan(FieldPath.documentId(), PlayerSearch.upperBound(key))
                    .limit(PlayerSearch.MAX_RESULTS.toLong()).get().await().documents
                    .forEach { d -> d.getString("uid")?.let(uids::add) }
            }
        }
        if (uids.size < 3) {
            firestore.collection("users").whereEqualTo("nickname", q).limit(5).get().await().documents.forEach { uids += it.id }
            for (prefix in PlayerSearch.nicknamePrefixes(q)) {
                if (uids.size >= PlayerSearch.MAX_RESULTS) break
                firestore.collection("users").orderBy("nickname")
                    .startAt(prefix).endAt(PlayerSearch.upperBound(prefix)).limit(PlayerSearch.MAX_RESULTS.toLong()).get().await()
                    .documents.forEach { uids += it.id }
            }
        }
        coroutineScope { uids.take(PlayerSearch.MAX_RESULTS).map { async { lite(it) } }.awaitAll().filterNotNull() }
    }

    private suspend fun lite(uid: String): PlayerInfo? {
        val user: DocumentSnapshot = firestore.collection("users").document(uid).get().await()
        if (!user.exists()) return null
        return PlayerInfo(
            uid = uid,
            nickname = user.getString("nickname").orEmpty(),
            level = user.getLong("level") ?: 0L,
            periodXp = user.getLong("periodXp") ?: 0L,
            totalXp = user.getLong("totalXp") ?: 0L,
            friendCode = user.getString("friendCode").orEmpty()
        )
    }

    /** The record of one account, every part read on its own so one that is refused leaves only its own gap. */
    suspend fun details(uid: String): PlayerDetails = coroutineScope {
        val now = System.currentTimeMillis()
        val strikes = async { runCatching { firestore.collection("moderationStrikes").document(uid).get().await().getLong("count") }.getOrNull() }
        val penalties = async {
            runCatching {
                firestore.collection("penalties").whereEqualTo("uid", uid).get().await().documents.map { d ->
                    PenaltyLine(
                        strike = (d.getLong("strike") ?: 0L).toInt(),
                        xpRevoked = d.getLong("xpRevoked") ?: 0L,
                        goldRevoked = d.getLong("goldRevoked") ?: 0L,
                        lockedUntilMillis = d.getLong("lockedUntil") ?: 0L,
                        createdAtMillis = d.getLong("createdAt") ?: 0L,
                        canceled = (d.getLong("canceledAt") ?: 0L) > 0L
                    )
                }.sortedByDescending { it.createdAtMillis }
            }.getOrNull()
        }
        val reports = async {
            // a count query: one read however many reports there are
            runCatching {
                firestore.collection("playerReports").whereEqualTo("reportedUid", uid)
                    .count().get(AggregateSource.SERVER).await().count.toInt()
            }.getOrNull()
        }
        val backup = async { runCatching { firestore.collection("users").document(uid).collection("backup").document("state").get().await() }.getOrNull() }
        val pool = async { runCatching { firestore.collection("ghostRuns").whereEqualTo("uid", uid).count().get(AggregateSource.SERVER).await().count }.getOrNull() }
        val queue = async { runCatching { firestore.collection("pendingRuns").whereEqualTo("uid", uid).count().get(AggregateSource.SERVER).await().count }.getOrNull() }
        val lines = penalties.await().orEmpty()
        val save = backup.await()?.takeIf { it.exists() }
        PlayerDetails(
            strikes = (strikes.await() ?: 0L).toInt(),
            lockedUntilMillis = lines.maxOfOrNull { it.lockedUntilMillis }?.takeIf { it > now } ?: 0L,
            reports = reports.await() ?: 0,
            penalties = lines.take(MAX_PENALTIES),
            goldBalance = save?.getLong("goldBalance"),
            lifetimeXp = save?.getLong("lifetimeXp"),
            lastSavedMillis = save?.getTimestamp("sealedAt")?.toDate()?.time,
            poolRuns = pool.await(),
            queueRuns = queue.await()
        )
    }

    /** The coupons among the newest [COUPONS_CHECKED] this account redeemed (one read each, only when asked for). */
    suspend fun couponUses(uid: String): Result<List<CouponUse>> = runCatching {
        val codes = firestore.collection("coupons").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(COUPONS_CHECKED).get().await().documents.map { it.id }
        coroutineScope {
            codes.map { code ->
                async {
                    runCatching { firestore.collection("coupons").document(code).collection("redemptions").document(uid).get().await() }
                        .getOrNull()?.takeIf { it.exists() }?.let { CouponUse(code, it.getLong("redeemedAt") ?: 0L) }
                }
            }.awaitAll().filterNotNull().sortedByDescending { it.redeemedAtMillis }
        }
    }

    private companion object {
        const val MAX_REPORTS = 400L
        const val MAX_PENALTIES = 8
        const val COUPONS_CHECKED = 25L
        const val MAX_GRANT = 100_000
    }
}
