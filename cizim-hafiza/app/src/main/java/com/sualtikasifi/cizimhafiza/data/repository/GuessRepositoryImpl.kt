package com.sualtikasifi.cizimhafiza.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke
import com.sualtikasifi.cizimhafiza.domain.model.GuessInboxEntry
import com.sualtikasifi.cizimhafiza.domain.model.GuessLink
import com.sualtikasifi.cizimhafiza.domain.repository.GuessRepository
import com.sualtikasifi.cizimhafiza.util.GuessCodec
import com.sualtikasifi.cizimhafiza.util.GuessStore
import com.sualtikasifi.cizimhafiza.util.TurkeyTime
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Firestore layout (see firestore.rules, written key for key to match): `guessLinks/{id}` for the links and
 * `users/{ownerUid}/guessInbox/{linkId}_{guesserUid}` for what came of them. Nothing here is read on a timer: a link
 * is read when it is opened, the inbox once when the home screen opens.
 */
class GuessRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val store: GuessStore
) : GuessRepository {

    private val links get() = firestore.collection("guessLinks")

    override val currentUid: String? get() = auth.currentUser?.uid

    private suspend fun requireUid(): String {
        auth.currentUser?.uid?.let { return it }
        return auth.signInAnonymously().await().user?.uid ?: throw IllegalStateException("Firebase oturumu açılamadı")
    }

    override suspend fun createLink(word: String, strokes: List<DrawingStroke>, seconds: Int, lang: String, ownerName: String): Result<String> =
        runCatching {
            if (store.linksLeftToday <= 0) throw GuessRepository.LimitReachedException()
            val uid = requireUid()
            val id = GuessCodec.newId()
            val salt = GuessCodec.newSalt()
            val now = System.currentTimeMillis()
            // The link and its line in the day's quota go in one batch: the rules refuse either without the other, which is what
            // holds the twenty-a-day limit on the server (the phone's own count above only saves the round trip).
            val quotaRef = firestore.collection("guessQuota").document(uid)
            val today = TurkeyTime.today().toEpochDay()
            val quota = runCatching { quotaRef.get().await() }.getOrNull()
            val used = if (quota != null && quota.exists() && quota.getLong("day") == today) (quota.getLong("count") ?: 0L) else 0L
            if (used >= GuessStore.MAX_LINKS_PER_DAY) throw GuessRepository.LimitReachedException()
            val batch = firestore.batch()
            batch.set(
                links.document(id),
                mapOf(
                    "ownerUid" to uid,
                    "ownerName" to ownerName.take(40),
                    "lang" to lang,
                    "salt" to salt,
                    "hash" to GuessCodec.hash(salt, word),
                    "pattern" to GuessCodec.pattern(word),
                    "first" to GuessCodec.firstLetter(word),
                    "strokesJson" to GuessCodec.encodeStrokes(strokes),
                    "seconds" to seconds.coerceIn(0, 120),
                    "createdAt" to now,
                    "expiresAt" to now + GuessCodec.LINK_LIFETIME_MILLIS
                )
            )
            batch.set(quotaRef, mapOf("day" to today, "count" to used + 1, "last" to id))
            batch.commit().await()
            store.recordLinkCreated()
            id
        }

    override suspend fun getLink(id: String): Result<GuessLink?> = runCatching {
        requireUid()
        val doc = links.document(id).get().await()
        if (!doc.exists()) return@runCatching null
        val link = GuessLink(
            id = id,
            ownerUid = doc.getString("ownerUid") ?: return@runCatching null,
            ownerName = doc.getString("ownerName").orEmpty(),
            lang = doc.getString("lang") ?: "tr",
            salt = doc.getString("salt") ?: return@runCatching null,
            hash = doc.getString("hash") ?: return@runCatching null,
            pattern = doc.getString("pattern").orEmpty(),
            first = doc.getString("first").orEmpty(),
            strokes = GuessCodec.decodeStrokes(doc.getString("strokesJson").orEmpty()),
            seconds = doc.getLong("seconds")?.toInt() ?: 0,
            createdAt = doc.getLong("createdAt") ?: 0L,
            expiresAt = doc.getLong("expiresAt") ?: 0L
        )
        link.takeIf { it.strokes.isNotEmpty() && it.expiresAt > System.currentTimeMillis() }
    }.recoverCatching { e ->
        // A link that has run out is refused by the rules as a permission error: to the player that is "gone", not "broken".
        if ((e as? com.google.firebase.firestore.FirebaseFirestoreException)?.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) null
        else throw e
    }

    override suspend fun sendResult(link: GuessLink, solved: Boolean, attempts: Int, myName: String): Result<Unit> = runCatching {
        val uid = requireUid()
        if (uid == link.ownerUid) return@runCatching
        val entryId = "${link.id}_$uid"
        val entry = firestore.collection("users").document(link.ownerUid).collection("guessInbox").document(entryId)
        entry.set(
            mapOf(
                "linkId" to link.id,
                "fromUid" to uid,
                "fromName" to myName.ifBlank { "?" }.take(40),
                "solved" to solved,
                "attempts" to attempts.coerceIn(1, GuessCodec.MAX_ATTEMPTS),
                "at" to System.currentTimeMillis()
            )
        ).await()
        Unit
    }

    override suspend fun fetchInbox(): Result<List<GuessInboxEntry>> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching emptyList()
        firestore.collection("users").document(uid).collection("guessInbox").limit(30).get().await().documents.mapNotNull { d ->
            GuessInboxEntry(
                id = d.id,
                linkId = d.getString("linkId") ?: return@mapNotNull null,
                fromUid = d.getString("fromUid").orEmpty(),
                fromName = d.getString("fromName").orEmpty(),
                solved = d.getBoolean("solved") ?: false,
                attempts = d.getLong("attempts")?.toInt() ?: 1,
                at = d.getLong("at") ?: 0L
            )
        }.sortedByDescending { it.at }
    }

    override suspend fun clearInbox(ids: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        val inbox = firestore.collection("users").document(uid).collection("guessInbox")
        ids.forEach { runCatching { inbox.document(it).delete().await() } }
    }

    override suspend fun deleteExpiredLinks() {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            val now = System.currentTimeMillis()
            links.whereEqualTo("ownerUid", uid).limit(40).get().await().documents
                .filter { (it.getLong("expiresAt") ?: 0L) <= now }
                .forEach { runCatching { it.reference.delete().await() } }
        }
    }
}
