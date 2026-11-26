package com.sualtikasifi.cizimhafiza.domain.model

/** What a gift holds. [key] is stored in Firestore (giftInbox) and must match firestore.rules. */
enum class GiftKind(val key: String) {
    GOLD("gold"), PEN("pen"), FRAME("frame"), JOKER("joker");

    companion object {
        fun of(key: String?): GiftKind? = entries.firstOrNull { it.key == key }
    }
}

/**
 * One thing a player can send. [cost] is what leaves the sender's wallet (and what the daily ceilings count); for
 * gold, the friend receives [receives] — the rest is the fee, burned. [itemId] is the pen/frame constant name or the
 * joker type name; empty for gold.
 */
data class GiftOffer(val kind: GiftKind, val itemId: String, val qty: Int, val cost: Int, val receives: Int)

/**
 * The limits of friend gifting. Every number here is also written into firestore.rules (giftInbox / giftStats) —
 * change them together. The rules are what holds them; this object lets the app say no before a round trip.
 */
object GiftRules {
    const val MAX_SENT_PER_DAY = 5000
    const val MAX_PER_FRIEND_PER_DAY = 1000
    const val MAX_RECEIVED_PER_DAY = 5000
    const val MAX_GIFTS_PER_DAY = 3
    const val MIN_SENDER_LEVEL = 10
    const val MIN_SENDER_ACCOUNT_DAYS = 7
    const val MIN_FRIENDSHIP_DAYS = 2
    const val MIN_COST = 100
    const val GOLD_FEE_PERCENT = 20
    const val ITEM_FEE_PERCENT = 10
    const val JOKER_PACK = 3
    val GOLD_AMOUNTS = listOf(100, 250, 500, 1000)

    fun goldReceived(cost: Int): Int = cost - cost * GOLD_FEE_PERCENT / 100

    /** What sending an item priced [price] costs: the price plus the fee, rounded up. */
    fun itemCost(price: Int): Int = (price * (100 + ITEM_FEE_PERCENT) + 99) / 100

    /** What a friend who already owns the item gets back instead, in gold. */
    fun ownedPayout(cost: Int): Int = cost * 70 / 100

    fun goldOffers(): List<GiftOffer> = GOLD_AMOUNTS.map { GiftOffer(GiftKind.GOLD, "", 1, it, goldReceived(it)) }

    /** Store pens and frames and a pack of jokers that fit under the per-friend ceiling. League, rune and friend-streak prizes are never here. */
    fun itemOffers(): List<GiftOffer> {
        val pens = PenSkin.entries.filter { it.isSoldInStore }.map { GiftOffer(GiftKind.PEN, it.name, 1, itemCost(it.storePrice), 0) }
        val frames = AvatarFrame.entries.filter { it.isSoldInStore }.map { GiftOffer(GiftKind.FRAME, it.name, 1, itemCost(it.storePrice), 0) }
        val jokers = JokerType.entries.map { GiftOffer(GiftKind.JOKER, it.name, JOKER_PACK, itemCost(it.price * JOKER_PACK), 0) }
        return (pens + frames + jokers).filter { it.cost in MIN_COST..MAX_PER_FRIEND_PER_DAY }.sortedBy { it.cost }
    }

    fun offers(): List<GiftOffer> = goldOffers() + itemOffers()
}

/** A gift waiting in this player's inbox. */
data class IncomingGift(
    val id: String,
    val fromUid: String,
    val fromName: String,
    val kind: GiftKind,
    val itemId: String,
    val qty: Int,
    val cost: Int,
    val gold: Int
)

/** What this player has sent today, as the server counted it. */
data class GiftUsage(val sentGold: Int, val sentCount: Int, val sentToFriend: Int) {
    val goldLeft: Int get() = (GiftRules.MAX_SENT_PER_DAY - sentGold).coerceAtLeast(0)
    val countLeft: Int get() = (GiftRules.MAX_GIFTS_PER_DAY - sentCount).coerceAtLeast(0)
    val friendLeft: Int get() = (GiftRules.MAX_PER_FRIEND_PER_DAY - sentToFriend).coerceAtLeast(0)
    fun canAfford(offer: GiftOffer): Boolean = countLeft > 0 && offer.cost <= goldLeft && offer.cost <= friendLeft
}

/** Why a gift could not be sent, so the window can say something useful. */
enum class GiftBlock { CHILD_MODE, NOT_LINKED, LEVEL, ACCOUNT_AGE, FRIENDSHIP_AGE, FRIEND_NOT_READY, DISABLED, OFFLINE, NO_GOLD, LIMIT, REFUSED }

class GiftException(val block: GiftBlock) : Exception(block.name)
