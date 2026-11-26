package com.sualtikasifi.cizimhafiza.domain.repository

import com.sualtikasifi.cizimhafiza.domain.model.GiftOffer
import com.sualtikasifi.cizimhafiza.domain.model.GiftUsage
import com.sualtikasifi.cizimhafiza.domain.model.IncomingGift

/** Gifts between friends (users/{uid}/giftInbox, users/{uid}/giftStats — see firestore.rules). */
interface GiftRepository {
    /** Stamps the account's join date once and whether it may receive gifts. Safe to call on every launch. */
    suspend fun publishProfile(giftsOk: Boolean)

    /** Whether gifting is switched on (config/gifts) and the friend can receive; null when it could not be read. */
    suspend fun friendReady(friendUid: String): Boolean?

    /** Today's counters as the server holds them; fails offline, which is what keeps a send from being queued blind. */
    suspend fun usage(friendUid: String): Result<GiftUsage>

    /** Takes the gold, writes the gift and its counters in one batch, and gives the gold back if the server refuses. */
    suspend fun send(friendUid: String, myName: String, offer: GiftOffer): Result<Unit>

    suspend fun fetchInbox(): Result<List<IncomingGift>>

    /** Credits the gift on this device (once per gift id) and removes it from the inbox. Returns what it was turned into. */
    suspend fun claim(gift: IncomingGift): ClaimResult

    sealed interface ClaimResult {
        data class Gold(val amount: Int) : ClaimResult
        data class Item(val gift: IncomingGift) : ClaimResult
        /** The item was already owned: paid out as gold instead. */
        data class Converted(val amount: Int) : ClaimResult
    }
}
