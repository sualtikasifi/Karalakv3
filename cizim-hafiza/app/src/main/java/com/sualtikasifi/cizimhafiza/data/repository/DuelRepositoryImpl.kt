package com.sualtikasifi.cizimhafiza.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import com.sualtikasifi.cizimhafiza.R
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.sualtikasifi.cizimhafiza.domain.model.Duel
import com.sualtikasifi.cizimhafiza.domain.model.DuelStatus
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import com.sualtikasifi.cizimhafiza.domain.model.StakeBlock
import com.sualtikasifi.cizimhafiza.domain.model.StakeException
import com.sualtikasifi.cizimhafiza.domain.model.StakeRules
import com.sualtikasifi.cizimhafiza.domain.model.StakeUsage
import com.sualtikasifi.cizimhafiza.domain.repository.StakeSettlement
import com.sualtikasifi.cizimhafiza.domain.repository.DuelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Firestore layout: a single top-level `duels/{duelId}` collection (not
 * nested under a user, since a duel has two participants — see
 * firestore.rules for the actual create/update/read enforcement, which
 * this repository's writes are shaped to match exactly, key for key).
 */
class DuelRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context,
    private val historyStore: com.sualtikasifi.cizimhafiza.util.DuelHistoryStore,
    private val settings: com.sualtikasifi.cizimhafiza.util.SettingsRepository,
    private val legal: com.sualtikasifi.cizimhafiza.util.LegalConsentStore
) : DuelRepository {

    private val duels get() = firestore.collection("duels")
    private val json = Json { ignoreUnknownKeys = true }

    override val currentUid: String? get() = auth.currentUser?.uid

    override val history: Flow<List<com.sualtikasifi.cizimhafiza.util.DuelHistoryEntry>> get() = historyStore.entries

    override fun recordReceivedResult(duel: Duel, myScore: Int, myCorrectCount: Int) =
        historyStore.archiveReceived(duel, myScore, myCorrectCount)

    override fun markHistorySeen(duelId: String) = historyStore.markSeen(duelId)

    /** Copies any finished duels a snapshot carries into the on-phone history. */
    private fun List<Duel>.archived(): List<Duel> {
        currentUid?.let { historyStore.archive(this, it) }
        return this
    }

    private suspend fun requireUid(): String {
        auth.currentUser?.uid?.let { return it }
        return auth.signInAnonymously().await().user?.uid
            ?: throw IllegalStateException("Firebase oturumu açılamadı")
    }

    override suspend fun createDuel(
        opponentUid: String,
        opponentName: String,
        items: List<ResultItem>,
        challengerScore: Int,
        challengerCorrectCount: Int,
        wordIds: List<Int>,
        stake: Int
    ): Result<Unit> = runCatching {
        val uid = requireUid()
        if (stake > 0) return@runCatching createWager(uid, opponentUid, opponentName, items, challengerScore, challengerCorrectCount, wordIds, stake)
        // Cache-first: this is the player's OWN profile doc, written by this
        // device (see FriendRepositoryImpl.publishLeagueScore/ensureFriendCode)
        // and read again on every duel sent. A server read per duel bought
        // nothing — the cached copy cannot be staler than this device's own
        // last write of it.
        val meDoc = firestore.collection("users").document(uid)
        val challengerName = (
            runCatching { meDoc.get(Source.CACHE).await() }.getOrNull()?.takeIf { it.exists() }
                ?: meDoc.get(Source.SERVER).await()
            ).getString("nickname").orEmpty().ifBlank { context.getString(R.string.default_nickname) }
        duels.add(
            mapOf(
                "challengerUid" to uid,
                "challengerName" to challengerName,
                "opponentUid" to opponentUid,
                "opponentName" to opponentName,
                "itemsJson" to json.encodeToString(items),
                "wordIds" to wordIds,
                "challengerScore" to challengerScore,
                "challengerCorrectCount" to challengerCorrectCount,
                "opponentScore" to null,
                "opponentCorrectCount" to null,
                "status" to DuelStatus.AWAITING_OPPONENT.name,
                "createdAt" to System.currentTimeMillis(),
                "completedAt" to null,
                // The challenger created it, so there is nothing about their
                // own duel they haven't already seen.
                "seenByChallenger" to true,
                // Markers for the scheduled push job (functions/src/duelPush.ts): null = "not announced yet", which is what it queries for.
                "pushedAt" to null,
                "resultPushedAt" to null
            )
        ).await()
        Unit
    }

    private fun dayKey(): String {
        val c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { add(java.util.Calendar.HOUR_OF_DAY, 3) }
        return (c.get(java.util.Calendar.YEAR) * 10000 + (c.get(java.util.Calendar.MONTH) + 1) * 100 + c.get(java.util.Calendar.DAY_OF_MONTH)).toString()
    }

    private fun stats(uid: String) = firestore.collection("users").document(uid).collection("stakeStats")

    override suspend fun stakeUsage(friendUid: String): Result<com.sualtikasifi.cizimhafiza.domain.model.StakeUsage> = runCatching {
        val uid = auth.currentUser?.uid ?: throw StakeException(StakeBlock.NOT_LINKED)
        val day = dayKey()
        val today = try { stats(uid).document(day).get(Source.SERVER).await() } catch (e: Exception) { throw StakeException(StakeBlock.OFFLINE) }
        val pair = try { stats(uid).document(day + "_" + friendUid).get(Source.SERVER).await() } catch (e: Exception) { throw StakeException(StakeBlock.OFFLINE) }
        StakeUsage((today.getLong("gold") ?: 0L).toInt(), (today.getLong("count") ?: 0L).toInt(), (pair.getLong("gold") ?: 0L).toInt())
    }

    override suspend fun friendStakeBlock(friendUid: String): StakeBlock? {
        val doc = runCatching { firestore.collection("users").document(friendUid).get(Source.SERVER).await() }.getOrNull() ?: return StakeBlock.OFFLINE
        return when {
            doc.getBoolean("stakeBan") == true -> StakeBlock.BANNED
            (doc.getLong("level") ?: 1L) < StakeRules.MIN_LEVEL -> StakeBlock.FRIEND_LEVEL
            doc.getBoolean("giftsOk") != true -> StakeBlock.FRIEND_NOT_READY
            else -> null
        }
    }

    /** Whether the wager switch (config/stakes) is on; true when it cannot be read — the rules hold it in any case. */
    private suspend fun wagersOn(): Boolean =
        runCatching { firestore.collection("config").document("stakes").get(Source.SERVER).await() }.getOrNull()
            ?.let { !it.exists() || it.getBoolean("enabled") != false } ?: true

    private suspend fun createWager(
        uid: String, opponentUid: String, opponentName: String, items: List<ResultItem>,
        challengerScore: Int, challengerCorrectCount: Int, wordIds: List<Int>, stake: Int
    ) {
        if (legal.childMode.value) throw StakeException(StakeBlock.CHILD_MODE)
        val user = auth.currentUser
        if (user == null || user.isAnonymous) throw StakeException(StakeBlock.NOT_LINKED)
        if (stake !in StakeRules.STAKES || !StakeRules.roundFits(wordIds.size)) throw StakeException(StakeBlock.REFUSED)
        if (settings.playerLevelNow() < StakeRules.MIN_LEVEL) throw StakeException(StakeBlock.LEVEL)
        val created = user.metadata?.creationTimestamp ?: 0L
        if (created > 0 && System.currentTimeMillis() - created < StakeRules.MIN_CHALLENGER_ACCOUNT_DAYS * 86_400_000L) throw StakeException(StakeBlock.ACCOUNT_AGE)
        if (!wagersOn()) throw StakeException(StakeBlock.DISABLED)
        if (settings.goldBalance.value < stake) throw StakeException(StakeBlock.NO_GOLD)
        friendStakeBlock(opponentUid)?.let { throw StakeException(it) }
        val used = stakeUsage(opponentUid).getOrThrow()
        if (!used.canStake(stake)) throw StakeException(StakeBlock.LIMIT)

        val challengerName = firestore.collection("users").document(uid).get(Source.SERVER).await()
            .getString("nickname").orEmpty().ifBlank { context.getString(R.string.default_nickname) }
        val ref = duels.document()
        val day = dayKey()
        val batch = firestore.batch()
        batch.set(
            ref,
            mapOf(
                "challengerUid" to uid, "challengerName" to challengerName, "opponentUid" to opponentUid, "opponentName" to opponentName,
                "itemsJson" to json.encodeToString(items), "wordIds" to wordIds,
                "challengerScore" to challengerScore, "challengerCorrectCount" to challengerCorrectCount,
                "opponentScore" to null, "opponentCorrectCount" to null,
                "status" to DuelStatus.AWAITING_OPPONENT.name, "createdAt" to System.currentTimeMillis(), "completedAt" to null,
                "seenByChallenger" to true, "pushedAt" to null, "resultPushedAt" to null,
                "stake" to stake, "stakeAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(), "cOpen" to true, "oOpen" to false
            )
        )
        batch.set(stats(uid).document(day), mapOf("gold" to used.gold + stake, "count" to used.count + 1, "lastDuel" to ref.id))
        batch.set(stats(uid).document(day + "_" + opponentUid), mapOf("gold" to used.withFriend + stake, "lastDuel" to ref.id))
        // The stake leaves the wallet first; a refused write gives it back.
        val taken = settings.revokeGold(stake)
        if (taken < stake) {
            if (taken > 0) settings.earnGold(taken, com.sualtikasifi.cizimhafiza.domain.model.EarnSource.GIFT)
            throw StakeException(StakeBlock.NO_GOLD)
        }
        try {
            kotlinx.coroutines.withTimeout(20_000) { batch.commit().await() }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            // Queued by Firestore and may still land: the stake stays taken (a refund could pay twice).
        } catch (e: Exception) {
            settings.earnGold(stake, com.sualtikasifi.cizimhafiza.domain.model.EarnSource.GIFT)
            throw StakeException(if ((e as? com.google.firebase.firestore.FirebaseFirestoreException)?.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) StakeBlock.REFUSED else StakeBlock.OFFLINE)
        }
    }

    override suspend fun acceptWager(duel: Duel): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw StakeException(StakeBlock.NOT_LINKED)
        if (legal.childMode.value) throw StakeException(StakeBlock.CHILD_MODE)
        if (auth.currentUser?.isAnonymous != false) throw StakeException(StakeBlock.NOT_LINKED)
        if (settings.playerLevelNow() < StakeRules.MIN_LEVEL) throw StakeException(StakeBlock.LEVEL)
        if (duel.stakeAtMillis > 0 && System.currentTimeMillis() - duel.stakeAtMillis >= StakeRules.ACCEPT_MILLIS) throw StakeException(StakeBlock.TOO_LATE)
        if (!wagersOn()) throw StakeException(StakeBlock.DISABLED)
        if (settings.goldBalance.value < duel.stake) throw StakeException(StakeBlock.NO_GOLD)
        val used = stakeUsage(duel.challengerUid).getOrThrow()
        if (!used.canStake(duel.stake)) throw StakeException(StakeBlock.LIMIT)
        val day = dayKey()
        val batch = firestore.batch()
        batch.update(duels.document(duel.id), mapOf("status" to DuelStatus.ACCEPTED.name, "acceptedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(), "oOpen" to true))
        batch.set(stats(uid).document(day), mapOf("gold" to used.gold + duel.stake, "count" to used.count + 1, "lastDuel" to duel.id))
        batch.set(stats(uid).document(day + "_" + duel.challengerUid), mapOf("gold" to used.withFriend + duel.stake, "lastDuel" to duel.id))
        val taken = settings.revokeGold(duel.stake)
        if (taken < duel.stake) {
            if (taken > 0) settings.earnGold(taken, com.sualtikasifi.cizimhafiza.domain.model.EarnSource.GIFT)
            throw StakeException(StakeBlock.NO_GOLD)
        }
        try {
            batch.commit().await()
        } catch (e: Exception) {
            settings.earnGold(duel.stake, com.sualtikasifi.cizimhafiza.domain.model.EarnSource.GIFT)
            throw StakeException(if ((e as? com.google.firebase.firestore.FirebaseFirestoreException)?.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) StakeBlock.TOO_LATE else StakeBlock.OFFLINE)
        }
    }

    override suspend fun declineWager(duelId: String): Result<Unit> = runCatching {
        duels.document(duelId).update("status", DuelStatus.DECLINED.name).await()
        Unit
    }

    private val settledPrefs by lazy { context.getSharedPreferences("duel_stakes", Context.MODE_PRIVATE) }

    override suspend fun settleWagers(): List<StakeSettlement> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        if (auth.currentUser?.isAnonymous != false) return emptyList()
        val out = mutableListOf<StakeSettlement>()
        val now = System.currentTimeMillis()
        suspend fun fetch(field: String, flag: String): List<Duel> =
            runCatching { duels.whereEqualTo(field, uid).whereEqualTo(flag, true).get(Source.SERVER).await().documents.mapNotNull { it.toDuel() } }
                .getOrDefault(emptyList())

        for (duel0 in fetch("challengerUid", "cOpen")) {
            var duel = duel0
            // Time passing ends an undecided wager: the challenger is the one who may say so (rules: duels update).
            val next = when {
                duel.status == DuelStatus.AWAITING_OPPONENT && now - duel.stakeAtMillis > StakeRules.ACCEPT_MILLIS -> DuelStatus.EXPIRED
                duel.status == DuelStatus.ACCEPTED && now - duel.acceptedAtMillis > StakeRules.FINISH_MILLIS + StakeRules.FINISH_GRACE_MILLIS + 5_000 -> DuelStatus.FORFEIT
                else -> null
            }
            if (next != null) {
                if (runCatching { duels.document(duel.id).update("status", next.name).await() }.isFailure) continue
                duel = duel.copy(status = next)
            }
            val pay = StakeRules.payout(duel, iAmChallenger = true) ?: continue
            if (credit(duel, "c", pay) && runCatching { duels.document(duel.id).update("cOpen", false).await() }.isSuccess) {
                out += StakeSettlement(duel.opponentName, duel.stake, pay, duel.status)
            }
        }
        for (duel in fetch("opponentUid", "oOpen")) {
            val pay = StakeRules.payout(duel, iAmChallenger = false) ?: continue
            if (credit(duel, "o", pay) && runCatching { duels.document(duel.id).update("oOpen", false).await() }.isSuccess) {
                out += StakeSettlement(duel.challengerName, duel.stake, pay, duel.status)
            }
        }
        return out
    }

    /** Pays [gold] into this wallet once per (duel, side); the id is remembered so a failed flag write cannot pay twice. */
    private fun credit(duel: Duel, side: String, gold: Int): Boolean {
        val key = "${duel.id}_$side"
        val done = settledPrefs.getStringSet("done", emptySet()).orEmpty()
        if (key !in done) {
            if (gold > 0) settings.earnGold(gold, com.sualtikasifi.cizimhafiza.domain.model.EarnSource.GIFT)
            settledPrefs.edit().putStringSet("done", (done + key).toList().takeLast(300).toSet()).apply()
        }
        return true
    }

    override suspend fun getDuel(duelId: String): Result<Duel?> = runCatching {
        duels.document(duelId).get().await().toDuel()
    }

    override fun observeIncomingDuels(): Flow<List<Duel>> =
        firestoreFlow("duels:incoming") { emit, onError ->
            val uid = auth.currentUser?.uid
            if (uid == null) {
                emit(emptyList())
                // No listener to actually attach — an empty registration
                // that removes cleanly, matching the shape firestoreFlow expects.
                return@firestoreFlow duels.limit(0).addSnapshotListener { _, _ -> }
            }
            // Equality filters only, and the ordering done here rather than on
            // the server. An orderBy on a third field turns this into a query
            // needing a composite index, and this project's indexes are not
            // deployed — the CI job that would deploy them has never had its
            // service-account secret. The query then failed outright and the
            // screen showed an empty list, which reads as "no duels".
            //
            // A player's outstanding duels are a handful, so sorting and
            // trimming them here costs nothing worth measuring.
            duels
                .whereEqualTo("opponentUid", uid)
                .whereEqualTo("status", DuelStatus.AWAITING_OPPONENT.name)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }
                    emit(snapshot?.documents.orEmpty().mapNotNull { it.toDuel() }.newestFirst())
                }
        }

    override fun observeCompletedReceivedDuels(): Flow<List<Duel>> =
        firestoreFlow("duels:receivedCompleted") { emit, onError ->
            val uid = auth.currentUser?.uid
            if (uid == null) {
                emit(emptyList())
                return@firestoreFlow duels.limit(0).addSnapshotListener { _, _ -> }
            }
            // Equality filters only (no composite index needed), sorted here.
            duels
                .whereEqualTo("opponentUid", uid)
                .whereEqualTo("status", DuelStatus.COMPLETE.name)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }
                    emit(snapshot?.documents.orEmpty().mapNotNull { it.toDuel() }.archived().newestFirst())
                }
        }

    override fun observeSentDuels(): Flow<List<Duel>> =
        firestoreFlow("duels:sent") { emit, onError ->
            val uid = auth.currentUser?.uid
            if (uid == null) {
                emit(emptyList())
                return@firestoreFlow duels.limit(0).addSnapshotListener { _, _ -> }
            }
            // Sorted and trimmed on the device, for the same reason as the
            // incoming list above.
            duels
                .whereEqualTo("challengerUid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }
                    emit(snapshot?.documents.orEmpty().mapNotNull { it.toDuel() }.archived().newestFirst())
                }
        }

    /**
     * Newest first, capped — the ordering the server used to do.
     *
     * Kept as one helper so both lists cannot drift apart, and applied after
     * decoding so a document that will not parse cannot take a slot in the
     * page it was never going to fill.
     */
    private fun List<Duel>.newestFirst(): List<Duel> =
        sortedByDescending { it.createdAt }.take(SENT_DUELS_LIMIT.toInt())

    override suspend fun submitDuelResult(duelId: String, opponentScore: Int, opponentCorrectCount: Int): Result<Unit> = runCatching {
        duels.document(duelId).update(
            mapOf(
                "status" to DuelStatus.COMPLETE.name,
                "opponentScore" to opponentScore,
                "opponentCorrectCount" to opponentCorrectCount,
                "completedAt" to System.currentTimeMillis(),
                "seenByChallenger" to false
            )
        ).await()
        Unit
    }

    override suspend fun markSeenByChallenger(duelId: String): Result<Unit> = runCatching {
        // See firestore.rules' duels/{duelId} update rule's second branch —
        // the challenger's one and only allowed write, on a COMPLETE duel,
        // touching only this field.
        duels.document(duelId).update("seenByChallenger", true).await()
        Unit
    }

    override suspend fun deleteDuel(duelId: String): Result<Unit> = runCatching {
        historyStore.remove(duelId)
        duels.document(duelId).delete().await()
        Unit
    }

    private fun DocumentSnapshot.toDuel(): Duel? {
        val challengerUid = getString("challengerUid") ?: return null
        val opponentUid = getString("opponentUid") ?: return null
        val itemsJson = getString("itemsJson") ?: return null
        val items = runCatching { json.decodeFromString<List<ResultItem>>(itemsJson) }.getOrDefault(emptyList())
        val status = getString("status")?.let { runCatching { DuelStatus.valueOf(it) }.getOrNull() }
            ?: DuelStatus.AWAITING_OPPONENT
        return Duel(
            id = id,
            challengerUid = challengerUid,
            challengerName = getString("challengerName").orEmpty(),
            opponentUid = opponentUid,
            opponentName = getString("opponentName").orEmpty(),
            items = items,
            wordIds = (get("wordIds") as? List<*>).orEmpty().mapNotNull { (it as? Number)?.toInt() },
            challengerScore = (get("challengerScore") as? Number)?.toInt() ?: 0,
            challengerCorrectCount = (get("challengerCorrectCount") as? Number)?.toInt() ?: 0,
            opponentScore = (get("opponentScore") as? Number)?.toInt(),
            opponentCorrectCount = (get("opponentCorrectCount") as? Number)?.toInt(),
            status = status,
            createdAt = (get("createdAt") as? Number)?.toLong() ?: 0L,
            completedAt = (get("completedAt") as? Number)?.toLong(),
            seenByChallenger = getBoolean("seenByChallenger") ?: true,
            stake = (get("stake") as? Number)?.toInt() ?: 0,
            stakeAtMillis = getTimestamp("stakeAt")?.toDate()?.time ?: 0L,
            acceptedAtMillis = getTimestamp("acceptedAt")?.toDate()?.time ?: 0L,
            challengerOpen = getBoolean("cOpen") ?: false,
            opponentOpen = getBoolean("oOpen") ?: false
        )
    }

    private companion object {
        // "Did they beat me" is only interesting recently — an unbounded
        // list of every duel ever sent would grow without limit for a
        // player who challenges friends often.
        const val SENT_DUELS_LIMIT = 30L
    }
}
