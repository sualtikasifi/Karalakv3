package com.sualtikasifi.cizimhafiza.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.domain.model.FriendStreak
import com.sualtikasifi.cizimhafiza.domain.model.FriendStreaks
import com.sualtikasifi.cizimhafiza.domain.model.StreakWrite
import com.sualtikasifi.cizimhafiza.domain.repository.FriendStreakRepository
import com.sualtikasifi.cizimhafiza.util.TurkeyTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * `friendStreaks/{smallerUid}_{largerUid}`. The transitions themselves live in [FriendStreaks] (pure, tested) and are
 * written inside a transaction here so two phones counting the same day at once cannot count it twice.
 */
class FriendStreakRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : FriendStreakRepository {

    private val streaks get() = firestore.collection("friendStreaks")

    override val currentUid: String? get() = auth.currentUser?.uid

    private fun today(): Int = TurkeyTime.today().toEpochDay().toInt()

    override fun observeStreaks(): Flow<List<FriendStreak>> =
        firestoreFlow("friendStreaks") { emit, onError ->
            val uid = auth.currentUser?.uid
            if (uid == null) {
                emit(emptyList())
                return@firestoreFlow streaks.limit(0).addSnapshotListener { _, _ -> }
            }
            streaks.whereArrayContains("uids", uid).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                emit(snapshot?.documents.orEmpty().mapNotNull { it.toStreak(uid) })
            }
        }

    override suspend fun recordDuel(friendUid: String): Result<Unit> = runCatching {
        val me = auth.currentUser?.uid ?: return@runCatching
        val ref = streaks.document(FriendStreaks.pairId(me, friendUid))
        val day = today()
        firestore.runTransaction { tx ->
            val write = FriendStreaks.afterDuel(tx.get(ref).toStreak(me), me, friendUid, day, System.currentTimeMillis())
            if (write is StreakWrite.Save) tx.set(ref, write.fields)
            null
        }.await()
        Unit
    }

    override suspend fun repair(friendUid: String): Result<Unit> = runCatching {
        val me = auth.currentUser?.uid ?: return@runCatching
        val ref = streaks.document(FriendStreaks.pairId(me, friendUid))
        val day = today()
        firestore.runTransaction { tx ->
            val current = tx.get(ref).toStreak(me)
            if (current != null) {
                val write = FriendStreaks.afterRepair(current, me, friendUid, day, System.currentTimeMillis())
                if (write is StreakWrite.Save) tx.set(ref, write.fields)
            }
            null
        }.await()
        Unit
    }

    override suspend fun remove(friendUid: String) {
        val me = auth.currentUser?.uid ?: return
        runCatching { streaks.document(FriendStreaks.pairId(me, friendUid)).delete().await() }
    }

    private fun DocumentSnapshot.toStreak(me: String): FriendStreak? {
        if (!exists()) return null
        val uids = (get("uids") as? List<*>)?.filterIsInstance<String>() ?: return null
        val friend = uids.firstOrNull { it != me } ?: return null
        return FriendStreak(
            pairId = id,
            friendUid = friend,
            count = getLong("count")?.toInt() ?: 0,
            best = getLong("best")?.toInt() ?: 0,
            run = getLong("run")?.toInt() ?: 1,
            lastDay = getLong("lastDay")?.toInt() ?: Int.MIN_VALUE / 2,
            pendingDay = getLong("pendingDay")?.toInt() ?: -1,
            repairs = getLong("repairs")?.toInt() ?: 0,
            repairedBy = getString("repairedBy"),
            updatedAt = getLong("updatedAt") ?: 0L
        )
    }
}
