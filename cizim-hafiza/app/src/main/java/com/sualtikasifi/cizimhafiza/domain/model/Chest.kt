package com.sualtikasifi.cizimhafiza.domain.model

import kotlinx.serialization.Serializable

/**
 * A kasa (chest) tier — the reward table itself, kept together rather than
 * scattered across GameConstants because unlike GameConstants' game-balance
 * numbers, these three together ARE the whole chest economy.
 */
@Serializable
enum class ChestTier(val unlockDurationMillis: Long, val goldReward: IntRange) {
    SILVER(2 * 60 * 60 * 1000L, 25..45),
    GOLD(3 * 60 * 60 * 1000L, 80..140),
    RARE(6 * 60 * 60 * 1000L, 260..480)
}

/**
 * One slot's contents — up to [ChestSlots.SLOT_COUNT] of these live in
 * SettingsRepository.chestSlots at once. [unlockStartedAtMillis] null means
 * still locked (tap to start counting down); non-null means counting
 * toward [ChestTier.unlockDurationMillis] past that moment.
 */
@Serializable
data class Chest(
    val id: String,
    val tier: ChestTier,
    val unlockStartedAtMillis: Long? = null
) {
    fun isReady(nowMillis: Long): Boolean =
        unlockStartedAtMillis != null && nowMillis >= unlockStartedAtMillis + tier.unlockDurationMillis
}

/** What opening a ready chest actually paid out. */
data class ChestReward(
    val tier: ChestTier,
    val gold: Int,
    val jokers: Map<JokerType, Int> = emptyMap(),
    /** A store pen won outright (legendary chests only) — already added to the player's collection. */
    val penDrop: PenSkin? = null
)

object ChestSlots {
    const val SLOT_COUNT = 4

    /** How much a watched rewarded ad takes off a running unlock (once per day). */
    const val SPEEDUP_MILLIS = 2 * 60 * 60 * 1000L

    // A 240-long cycle, 75% / 20% / 5% — a per-account SHUFFLE of a fixed
    // multiset rather than a fresh weighted roll every time. A genuinely
    // random roll can hand out zero rares in a hundred chests and feel
    // broken even though it technically isn't; a shuffled fixed cycle
    // guarantees the long-run rate while still hiding exactly when the next
    // rare falls. See SettingsRepository.chestCycleSeed/chestCycleIndex for
    // how [seed] and [cycleIndex] are chosen and advanced.
    const val CYCLE_LENGTH = 240
    private const val SILVER_COUNT = 180
    private const val GOLD_COUNT = 48
    private const val RARE_COUNT = 12

    fun tierAt(seed: Long, cycleIndex: Int): ChestTier {
        val bag = buildList {
            repeat(SILVER_COUNT) { add(ChestTier.SILVER) }
            repeat(GOLD_COUNT) { add(ChestTier.GOLD) }
            repeat(RARE_COUNT) { add(ChestTier.RARE) }
        }.shuffled(kotlin.random.Random(seed))
        return bag[((cycleIndex % CYCLE_LENGTH) + CYCLE_LENGTH) % CYCLE_LENGTH]
    }
}

/**
 * What a chest pays out — the rarer the chest, the more gold, the more
 * jokers, and (for the top tier only) a chance at a store pen.
 *
 *  - Çırak (common):    25–45 gold;   30% chance of 1 joker.
 *  - Sanatçı (rare):    80–140 gold;  1 joker guaranteed, 30% chance of a 2nd.
 *  - Sürpriz (legendary): 260–480 gold; 2 jokers guaranteed, 40% chance of a 3rd,
 *                         and a 12% chance of a store pen worth up to 6.500 gold
 *                         that the player does not own yet.
 *
 * Which joker each roll gives is uniform over [JokerType].
 */
object ChestLoot {
    private const val PEN_DROP_CHANCE = 0.12f
    private const val PEN_DROP_MAX_PRICE = 6_500

    fun roll(tier: ChestTier, ownedStoreIds: Set<String>, rnd: kotlin.random.Random = kotlin.random.Random): ChestReward {
        val gold = tier.goldReward.random(rnd)
        val jokerRolls = when (tier) {
            ChestTier.SILVER -> if (rnd.nextFloat() < 0.30f) 1 else 0
            ChestTier.GOLD -> 1 + if (rnd.nextFloat() < 0.30f) 1 else 0
            ChestTier.RARE -> 2 + if (rnd.nextFloat() < 0.40f) 1 else 0
        }
        val jokers = mutableMapOf<JokerType, Int>()
        repeat(jokerRolls) {
            val type = JokerType.entries.random(rnd)
            jokers[type] = (jokers[type] ?: 0) + 1
        }
        val pen = if (tier == ChestTier.RARE && rnd.nextFloat() < PEN_DROP_CHANCE) {
            PenSkin.entries
                .filter { it.isStoreItem && it.storePrice <= PEN_DROP_MAX_PRICE && "pen:${it.name}" !in ownedStoreIds }
                .randomOrNull(rnd)
        } else null
        return ChestReward(tier, gold, jokers, pen)
    }
}
