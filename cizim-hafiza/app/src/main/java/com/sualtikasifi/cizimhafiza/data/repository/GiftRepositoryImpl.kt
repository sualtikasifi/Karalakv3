package com.sualtikasifi.cizimhafiza.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.sualtikasifi.cizimhafiza.domain.model.EarnSource
import com.sualtikasifi.cizimhafiza.domain.model.GiftBlock
import com.sualtikasifi.cizimhafiza.domain.model.GiftException
import com.sualtikasifi.cizimhafiza.domain.model.GiftKind
import com.sualtikasifi.cizimhafiza.domain.model.GiftOffer
import com.sualtikasifi.cizimhafiza.domain.model.GiftRules
import com.sualtikasifi.cizimhafiza.domain.model.GiftUsage
import com.sualtikasifi.cizimhafiza.domain.model.IncomingGift
import com.sualtikasifi.cizimhafiza.domain.model.JokerType
import com.sualtikasifi.cizimhafiza.domain.repository.GiftRepository
import com.sualtikasifi.cizimhafiza.util.LegalConsentStore
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

/**
 * Sending: check the limits locally (cheap, and the wording), read today's counters from the SERVER (so a send is never
 * queued blind while offline), take the gold, write the gift and its three counters in ONE batch — firestore.rules accept
 * it only if every counter moved by exactly the gift's cost — and hand the gold back if the batch is refused.
 * Receiving: credit on this device once per gift id (the id is remembered), then delete the inbox document.
 */
class GiftRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settings: SettingsRepository,
    private val legal: LegalConsentStore
) : GiftRepository {

    private val prefs by lazy { context.getSharedPreferences("gifts", Context.MODE_PRIVATE) }
    private fun users() = firestore.collection("users")

    override suspend fun publishProfile(giftsOk: Boolean) {
        val user = auth.currentUser ?: return
        val ref = users().document(user.uid)
        val stored = runCatching { ref.get(Source.SERVER).await() }.getOrNull() ?: return
        val data = mutableMapOf<String, Any>()
        if (stored.getBoolean("giftsOk") != giftsOk) data["giftsOk"] = giftsOk
        // The rules let this be written once and never moved; the sender's week-old account is measured from it.
        if (stored.getLong("joinedAt") == null) data["joinedAt"] = user.metadata?.creationTimestamp ?: System.currentTimeMillis()
        if (data.isEmpty() || !stored.exists()) return
        runCatching { ref.set(data, SetOptions.merge()).await() }
    }

    override suspend fun friendReady(friendUid: String): Boolean? {
        val doc = runCatching { users().document(friendUid).get(Source.SERVER).await() }.getOrNull() ?: return null
        return doc.getBoolean("giftsOk") == true
    }

    private fun dayKey(): String {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { add(Calendar.HOUR_OF_DAY, 3) }
        return (c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)).toString()
    }

    override suspend fun usage(friendUid: String): Result<GiftUsage> = runCatching {
        val uid = auth.currentUser?.uid ?: throw GiftException(GiftBlock.NOT_LINKED)
        val day = dayKey()
        val stats = users().document(uid).collection("giftStats")
        val today = try { stats.document(day).get(Source.SERVER).await() } catch (e: Exception) { throw GiftException(GiftBlock.OFFLINE) }
        val pair = try { stats.document(day + "_" + friendUid).get(Source.SERVER).await() } catch (e: Exception) { throw GiftException(GiftBlock.OFFLINE) }
        GiftUsage(
            sentGold = (today.getLong("gold") ?: 0L).toInt(),
            sentCount = (today.getLong("count") ?: 0L).toInt(),
            sentToFriend = (pair.getLong("gold") ?: 0L).toInt()
        )
    }

    override suspend fun send(friendUid: String, myName: String, offer: GiftOffer): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw GiftException(GiftBlock.NOT_LINKED)
        if (legal.childMode.value) throw GiftException(GiftBlock.CHILD_MODE)
        if (user.isAnonymous) throw GiftException(GiftBlock.NOT_LINKED)
        if (settings.playerLevelNow() < GiftRules.MIN_SENDER_LEVEL) throw GiftException(GiftBlock.LEVEL)
        val created = user.metadata?.creationTimestamp ?: 0L
        if (created > 0 && System.currentTimeMillis() - created < GiftRules.MIN_SENDER_ACCOUNT_DAYS * DAY_MS) throw GiftException(GiftBlock.ACCOUNT_AGE)
        if (offer.cost !in GiftRules.MIN_COST..GiftRules.MAX_PER_FRIEND_PER_DAY) throw GiftException(GiftBlock.REFUSED)
        if (settings.goldBalance.value < offer.cost) throw GiftException(GiftBlock.NO_GOLD)
        val used = usage(friendUid).getOrThrow()
        if (!used.canAfford(offer)) throw GiftException(GiftBlock.LIMIT)
        val friendEntry = runCatching { users().document(user.uid).collection("friends").document(friendUid).get().await() }.getOrNull()
        val since = friendEntry?.getLong("addedAt") ?: 0L
        if (since > 0 && System.currentTimeMillis() - since < GiftRules.MIN_FRIENDSHIP_DAYS * DAY_MS) throw GiftException(GiftBlock.FRIENDSHIP_AGE)
        if (friendReady(friendUid) == false) throw GiftException(GiftBlock.FRIEND_NOT_READY)

        // The gold leaves first; a refused write gives it back.
        val taken = settings.revokeGold(offer.cost)
        if (taken < offer.cost) {
            if (taken > 0) settings.earnGold(taken, EarnSource.GIFT)
            throw GiftException(GiftBlock.NO_GOLD)
        }
        val id = UUID.randomUUID().toString().replace("-", "").take(20)
        val day = dayKey()
        val gift = hashMapOf<String, Any>(
            "fromUid" to user.uid, "fromName" to myName.take(40), "kind" to offer.kind.key, "itemId" to offer.itemId,
            "qty" to offer.qty, "cost" to offer.cost, "gold" to offer.receives, "at" to FieldValue.serverTimestamp()
        )
        val stats = users().document(user.uid).collection("giftStats")
        val batch = firestore.batch()
        batch.set(users().document(friendUid).collection("giftInbox").document(id), gift)
        batch.set(stats.document(day), mapOf("gold" to used.sentGold + offer.cost, "count" to used.sentCount + 1, "lastGift" to id, "lastTo" to friendUid))
        batch.set(stats.document(day + "_" + friendUid), mapOf("gold" to used.sentToFriend + offer.cost, "lastGift" to id))
        // The friend's received-today counter: readable by anyone signed in (rules), so the new sum is built from it.
        batch.set(users().document(friendUid).collection("giftStats").document("in_$day"), mapOf("gold" to receivedSoFar(friendUid, day) + offer.cost, "lastGift" to id))
        try {
            withTimeout(COMMIT_TIMEOUT_MS) { batch.commit().await() }
        } catch (e: TimeoutCancellationException) {
            // Still queued by Firestore: it may yet land, so the gold stays taken (giving it back could pay twice).
        } catch (e: Exception) {
            settings.earnGold(offer.cost, EarnSource.GIFT)
            val denied = (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            throw GiftException(if (denied) GiftBlock.REFUSED else GiftBlock.OFFLINE)
        }
    }

    private suspend fun receivedSoFar(friendUid: String, day: String): Int {
        val doc = runCatching { users().document(friendUid).collection("giftStats").document("in_$day").get(Source.SERVER).await() }.getOrNull()
        return (doc?.getLong("gold") ?: 0L).toInt()
    }

    override suspend fun fetchInbox(): Result<List<IncomingGift>> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching emptyList()
        users().document(uid).collection("giftInbox").limit(20).get(Source.SERVER).await().documents.mapNotNull { d ->
            val kind = GiftKind.of(d.getString("kind")) ?: return@mapNotNull null
            IncomingGift(
                id = d.id,
                fromUid = d.getString("fromUid") ?: return@mapNotNull null,
                fromName = d.getString("fromName") ?: "?",
                kind = kind,
                itemId = d.getString("itemId") ?: "",
                qty = (d.getLong("qty") ?: 1L).toInt(),
                cost = (d.getLong("cost") ?: 0L).toInt(),
                gold = (d.getLong("gold") ?: 0L).toInt()
            )
        }
    }

    override suspend fun claim(gift: IncomingGift): GiftRepository.ClaimResult {
        val uid = auth.currentUser?.uid
        val claimed = prefs.getStringSet(KEY_CLAIMED, emptySet()).orEmpty()
        val result: GiftRepository.ClaimResult = if (gift.id in claimed) {
            GiftRepository.ClaimResult.Converted(0)
        } else when (gift.kind) {
            GiftKind.GOLD -> {
                settings.earnGold(gift.gold, EarnSource.GIFT)
                GiftRepository.ClaimResult.Gold(gift.gold)
            }
            GiftKind.PEN, GiftKind.FRAME -> {
                val storeId = (if (gift.kind == GiftKind.PEN) "pen:" else "frame:") + gift.itemId
                if (storeId in settings.ownedStoreIds.value) {
                    val back = GiftRules.ownedPayout(gift.cost)
                    settings.earnGold(back, EarnSource.GIFT)
                    GiftRepository.ClaimResult.Converted(back)
                } else {
                    settings.grantStoreItem(storeId)
                    GiftRepository.ClaimResult.Item(gift)
                }
            }
            GiftKind.JOKER -> {
                val type = JokerType.entries.firstOrNull { it.name == gift.itemId }
                if (type != null) settings.addJoker(type, gift.qty)
                GiftRepository.ClaimResult.Item(gift)
            }
        }
        prefs.edit().putStringSet(KEY_CLAIMED, (claimed + gift.id).toList().takeLast(200).toSet()).apply()
        if (uid != null) runCatching { users().document(uid).collection("giftInbox").document(gift.id).delete().await() }
        return result
    }

    private companion object {
        const val DAY_MS = 86_400_000L
        const val COMMIT_TIMEOUT_MS = 20_000L
        const val KEY_CLAIMED = "claimed"
    }
}
