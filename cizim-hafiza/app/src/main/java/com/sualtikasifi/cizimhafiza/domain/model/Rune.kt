package com.sualtikasifi.cizimhafiza.domain.model

import com.sualtikasifi.cizimhafiza.R

/** A branch of the rune map; [ROOT] is the centre every path starts from. */
enum class RuneBranch { ROOT, GOLD, CHEST, LUCK, GREEN, BLUE, PURPLE, CYAN, LEGEND }

/** How an effect's number is written: "+%5", "+3 puan", "+1 adet", or just "on" for a one-off ability. */
enum class RuneUnit { PERCENT, POINTS, COUNT, FLAG }

/**
 * What a rune does. Many runes share one effect and their levels add up, so a long map needs only a handful of
 * real effects. [iconKey] names the icon (a drawable called rune_icon_<iconKey> when present, otherwise [emoji]).
 * None of these touches match scoring or league points, so the league stays fair (the XP runes only speed up the player's level).
 */
enum class RuneEffect(val iconKey: String, val emoji: String, val unit: RuneUnit, val descRes: Int) {
    NONE("dog", "🐶", RuneUnit.PERCENT, R.string.rune_root_desc),
    WORD("pencil", "✏️", RuneUnit.PERCENT, R.string.rune_fx_word_desc),
    WIN("trophy", "🏆", RuneUnit.PERCENT, R.string.rune_fx_win_desc),
    LOGIN("gift", "🎁", RuneUnit.PERCENT, R.string.rune_fx_login_desc),
    DAILY("calendar", "📅", RuneUnit.PERCENT, R.string.rune_fx_daily_desc),
    CHEST("chest", "📦", RuneUnit.PERCENT, R.string.rune_fx_chest_desc),
    CJOKER("joker", "🃏", RuneUnit.POINTS, R.string.rune_fx_cjoker_desc),
    DISC("tag", "🏷️", RuneUnit.PERCENT, R.string.rune_fx_disc_desc),
    DJ("cards", "🎴", RuneUnit.COUNT, R.string.rune_fx_dj_desc),
    XP("owl", "🦉", RuneUnit.PERCENT, R.string.rune_fx_xp_desc),
    CTIME("stopwatch", "⏱️", RuneUnit.PERCENT, R.string.rune_fx_ctime_desc),
    ADCD("play", "▶️", RuneUnit.PERCENT, R.string.rune_fx_adcd_desc),
    ROOM("friends", "👥", RuneUnit.PERCENT, R.string.rune_fx_room_desc),
    HOST("house", "🏠", RuneUnit.PERCENT, R.string.rune_fx_host_desc),
    STREAK("chain", "⛓️", RuneUnit.FLAG, R.string.rune_fx_streak_desc),
    INSTANT("key", "🔑", RuneUnit.FLAG, R.string.rune_fx_instant_desc),
    DICE("dice", "🎲", RuneUnit.FLAG, R.string.rune_fx_dice_desc),
    RESERVE("slot", "🗃️", RuneUnit.FLAG, R.string.rune_fx_reserve_desc),
    BEST("bond", "💞", RuneUnit.FLAG, R.string.rune_fx_best_desc),
    INSURE("shield", "🛡️", RuneUnit.FLAG, R.string.rune_fx_insure_desc),
    STYLE("brush", "🖌️", RuneUnit.FLAG, R.string.rune_fx_style_desc);
}

/**
 * One node of the rune map ("Karalak Rünleri"): bought with gold, level by level, for a small permanent bonus.
 *
 * [id] is persisted (owned levels are stored by it) — never rename an existing one. [x]/[y] are the node's place on
 * the map in map units (the centre is 0,0; one unit is a node spacing). A node opens once everything in [requires] has
 * been bought at least once. A node with a single level is a "keystone": one big purchase at the end of a path.
 */
data class Rune(
    val id: String,
    val branch: RuneBranch,
    val effect: RuneEffect,
    val nameRes: Int,
    /** What each level adds (percent, points or pieces, see [RuneEffect.unit]). */
    val perLevel: Int,
    /** Gold for level 1, 2, 3 … — its size is the max level. */
    val costs: List<Int>,
    val requires: List<String>,
    val x: Float,
    val y: Float,
    /** Which ring of the map it belongs to: 1 is the first map, later rings open with the player's level and progress. */
    val ring: Int = 1,
    /** Null: a single-level rune is a keystone. Set it to false for a single-level rune that is not one (the style runes). */
    val keystoneFlag: Boolean? = null
) {
    val maxLevel: Int get() = costs.size
    val isKeystone: Boolean get() = keystoneFlag ?: (costs.size == 1)
}

object RuneTree {
    const val ROOT_ID = "root"
    const val WORD_GOLD = "word_gold"
    const val WIN_GOLD = "win_gold"
    const val LOGIN_GOLD = "login_gold"
    const val CHEST_GOLD = "chest_gold"
    const val CHEST_JOKER = "chest_joker"

    /**
     * One node per effect, with few but noticeable levels, and every path ends in an ability. Ring 1: gold, chests,
     * store. Ring 2 grows out of their keystones: the streak, time and friends paths. Ring 3: experience and the rune-only
     * pens and frames; the Legend ring is the crown at the end of it.
     */
    val all: List<Rune> = listOf(
        Rune(ROOT_ID, RuneBranch.ROOT, RuneEffect.NONE, R.string.rune_root_name, 0, listOf(0), emptyList(), 0f, 0f),
        // Ring 1 — Altın yolu
        Rune(WORD_GOLD, RuneBranch.GOLD, RuneEffect.WORD, R.string.rune_word_gold_name, 8, listOf(300, 700, 1_500, 3_000, 6_000), listOf(ROOT_ID), 0.000f, -1.000f),
        Rune(WIN_GOLD, RuneBranch.GOLD, RuneEffect.WIN, R.string.rune_win_gold_name, 10, listOf(1_000, 2_500, 5_000, 10_000), listOf(WORD_GOLD), 0.000f, -2.000f),
        Rune("streak_key", RuneBranch.GOLD, RuneEffect.STREAK, R.string.rune_streak_key_name, 0, listOf(30_000), listOf(WIN_GOLD), 0.000f, -3.100f),
        // Ring 1 — Kasa yolu
        Rune(CHEST_GOLD, RuneBranch.CHEST, RuneEffect.CHEST, R.string.rune_chest_gold_name, 10, listOf(300, 700, 1_500, 3_000, 6_000), listOf(ROOT_ID), -0.866f, 0.500f),
        Rune(CHEST_JOKER, RuneBranch.CHEST, RuneEffect.CJOKER, R.string.rune_chest_joker_name, 5, listOf(1_000, 2_500, 5_000, 10_000), listOf(CHEST_GOLD), -1.732f, 1.000f),
        Rune("chest_key2", RuneBranch.CHEST, RuneEffect.INSTANT, R.string.rune_chest_key2_name, 0, listOf(30_000), listOf(CHEST_JOKER), -2.680f, 1.550f),
        // Ring 1 — Mağaza yolu
        Rune("disc_1", RuneBranch.LUCK, RuneEffect.DISC, R.string.rune_disc_1_name, 10, listOf(500, 1_200, 2_500, 5_000), listOf(ROOT_ID), 0.866f, 0.500f),
        Rune("dj_1", RuneBranch.LUCK, RuneEffect.DJ, R.string.rune_dj_1_name, 1, listOf(2_000, 5_000, 10_000), listOf("disc_1"), 1.732f, 1.000f),
        Rune("luck_key", RuneBranch.LUCK, RuneEffect.DICE, R.string.rune_luck_key_name, 0, listOf(30_000), listOf("dj_1"), 2.680f, 1.550f),
        // Ring 2 — Seri yolu
        Rune(LOGIN_GOLD, RuneBranch.CYAN, RuneEffect.LOGIN, R.string.rune_login_gold_name, 10, listOf(3_000, 6_000, 10_000, 15_000, 22_000), listOf("streak_key"), -0.800f, -4.000f, 2),
        Rune("daily_1", RuneBranch.CYAN, RuneEffect.DAILY, R.string.rune_daily_1_name, 10, listOf(3_000, 6_000, 10_000, 15_000, 22_000), listOf("streak_key"), 0.800f, -4.000f, 2),
        Rune("ins_key", RuneBranch.CYAN, RuneEffect.INSURE, R.string.rune_ins_key_name, 0, listOf(60_000), listOf(LOGIN_GOLD, "daily_1"), 0.000f, -4.900f, 2),
        // Ring 2 — Zaman yolu
        Rune("time_1", RuneBranch.GREEN, RuneEffect.CTIME, R.string.rune_time_1_name, 10, listOf(3_000, 7_000, 12_000, 20_000), listOf("chest_key2"), -3.150f, 2.740f, 2),
        Rune("ad_1", RuneBranch.GREEN, RuneEffect.ADCD, R.string.rune_ad_1_name, 10, listOf(4_000, 10_000, 18_000), listOf("chest_key2"), -3.950f, 1.360f, 2),
        Rune("slot_key", RuneBranch.GREEN, RuneEffect.RESERVE, R.string.rune_slot_key_name, 0, listOf(50_000), listOf("time_1", "ad_1"), -4.330f, 2.500f, 2),
        // Ring 2 — Arkadaş yolu
        Rune("fr_1", RuneBranch.BLUE, RuneEffect.ROOM, R.string.rune_fr_1_name, 10, listOf(3_000, 7_000, 12_000, 20_000), listOf("luck_key"), 3.150f, 2.740f, 2),
        Rune("fr_2", RuneBranch.BLUE, RuneEffect.HOST, R.string.rune_fr_2_name, 10, listOf(4_000, 10_000, 18_000), listOf("luck_key"), 3.950f, 1.360f, 2),
        Rune("fr_key", RuneBranch.BLUE, RuneEffect.BEST, R.string.rune_fr_key_name, 0, listOf(50_000), listOf("fr_1", "fr_2"), 4.330f, 2.500f, 2),
        // Ring 3 — Ustalık yolu: experience. The path ends here for now; the pens and frames that used to hang off it
        // (style runes) were taken off the map, and more runes will be added in later updates.
        Rune("xp_1", RuneBranch.PURPLE, RuneEffect.XP, R.string.rune_xp_1_name, 6, listOf(8_000, 14_000, 22_000, 32_000, 45_000), listOf("ins_key"), 0.000f, -6.000f, 3)
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): Rune? = byId[id]

    /** Level the player holds; the root is always held. */
    fun levelOf(levels: Map<String, Int>, id: String): Int =
        if (id == ROOT_ID) 1 else (levels[id] ?: 0).coerceIn(0, byId[id]?.maxLevel ?: 0)

    /** The neighbours are bought: the rune could be bought if its ring is open too (see [canBuy]). */
    fun isUnlocked(levels: Map<String, Int>, rune: Rune): Boolean = rune.requires.all { levelOf(levels, it) >= 1 }

    /** How much of ring [ring] is bought, 0..1: bought levels over all levels (the root is not part of any ring). */
    fun ringProgress(levels: Map<String, Int>, ring: Int): Float {
        val members = all.filter { it.ring == ring && it.id != ROOT_ID }
        if (members.isEmpty()) return 1f
        val total = members.sumOf { it.maxLevel }
        val bought = members.sumOf { minOf(levelOf(levels, it.id), it.maxLevel) }
        return bought.toFloat() / total
    }

    /** Ring [ring] is open: the player's level is high enough and the ring before it is far enough along. Ring 1 is always open. */
    fun ringOpen(levels: Map<String, Int>, ring: Int, playerLevel: Int): Boolean {
        if (ring <= 1) return true
        return playerLevel >= RuneGates.levelFor(ring) && ringProgress(levels, ring - 1) >= RuneGates.percentNeeded / 100f
    }

    /** The rune can be bought now apart from the gold: its neighbours are bought and its ring is open. */
    fun canBuy(levels: Map<String, Int>, rune: Rune, playerLevel: Int): Boolean =
        isUnlocked(levels, rune) && ringOpen(levels, rune.ring, playerLevel)

    /** Gold for the next level, or null at the max (and for the root). */
    fun nextCost(levels: Map<String, Int>, rune: Rune): Int? {
        if (rune.id == ROOT_ID) return null
        val level = levelOf(levels, rune.id)
        return RuneGates.price(rune, level)
    }

    /** True when something can be bought right now with [gold]. */
    fun anyAffordable(levels: Map<String, Int>, gold: Int, playerLevel: Int): Boolean =
        all.any { r -> canBuy(levels, r, playerLevel) && (nextCost(levels, r) ?: Int.MAX_VALUE) <= gold }

    /** Total gold of the whole map. */
    val totalCost: Int get() = all.sumOf { r -> r.costs.indices.sumOf { RuneGates.price(r, it) ?: 0 } }

    /** Rune levels bought so far (the root, which every player holds, is not one of them). */
    fun boughtLevels(levels: Map<String, Int>): Int = all.filter { it.id != ROOT_ID }.sumOf { levelOf(levels, it.id) }

    /** The runes that could be bought now, gold apart, with the price of their next level. */
    private fun buyable(levels: Map<String, Int>, playerLevel: Int): List<Pair<Rune, Int>> =
        all.mapNotNull { r -> if (canBuy(levels, r, playerLevel)) nextCost(levels, r)?.let { r to it } else null }

    /**
     * What the home screen's rune sign says: how many runes the gold buys right now, or how much gold the cheapest next
     * one still needs, or the level the next ring waits for, or that the map is done.
     */
    fun hint(levels: Map<String, Int>, gold: Int, playerLevel: Int): RuneHint {
        val open = buyable(levels, playerLevel)
        val affordable = open.count { (_, cost) -> cost <= gold }
        if (affordable > 0) return RuneHint.Affordable(affordable)
        open.minOfOrNull { it.second }?.let { return RuneHint.Saving(it - gold) }
        val nextRing = all.filter { it.id != ROOT_ID && levelOf(levels, it.id) < it.maxLevel }.minOfOrNull { it.ring }
            ?: return RuneHint.Complete
        val level = RuneGates.levelFor(nextRing)
        return if (playerLevel < level) RuneHint.Locked(level) else RuneHint.Complete
    }

    /**
     * The rune a first-time guide points at: Kalem Parası (more gold for every right word, felt in the very next game)
     * when it can be bought now, otherwise the cheapest rune the gold buys; null when the gold buys nothing.
     */
    fun firstPick(levels: Map<String, Int>, gold: Int, playerLevel: Int): Rune? {
        val affordable = buyable(levels, playerLevel).filter { (_, cost) -> cost <= gold }
        return affordable.firstOrNull { it.first.id == WORD_GOLD }?.first ?: affordable.minByOrNull { it.second }?.first
    }
}

/** See [RuneTree.hint]. */
sealed interface RuneHint {
    /** [count] runes can be bought with the gold held now. */
    data class Affordable(val count: Int) : RuneHint
    /** The cheapest rune open now needs [goldNeeded] more gold. */
    data class Saving(val goldNeeded: Int) : RuneHint
    /** Nothing is open until the player reaches [level]. */
    data class Locked(val level: Int) : RuneHint
    /** Every rune there is has been bought to its last level. */
    data object Complete : RuneHint
}

/**
 * When the later rings of the map open: a player level and a share of the ring before it. The defaults live here;
 * the app can overwrite them from a server setting (see RuneConfigRepository) so the pace can be tuned without an update.
 */
object RuneGates {
    @Volatile var ring2Level: Int = 15
    @Volatile var ring3Level: Int = 30
    @Volatile var ring4Level: Int = 40
    @Volatile var percentNeeded: Int = 70
    /** Multiplies every rune price of rings 2 and up (1.0 = as designed). */
    @Volatile var priceFactor: Float = 1.0f

    /** Whether the mascot offers a player who has never bought a rune to show them their first one (see MainMenuViewModel). */
    @Volatile var firstRunePrompt: Boolean = true

    /** Days the mascot keeps quiet after the player closes that offer. */
    @Volatile var promptSnoozeDays: Int = 3

    fun levelFor(ring: Int): Int = when (ring) {
        2 -> ring2Level
        3 -> ring3Level
        else -> ring4Level
    }

    /** The price of [rune]'s level [level] (0-based) with the server's factor applied to the later rings. */
    fun price(rune: Rune, level: Int): Int? {
        val base = rune.costs.getOrNull(level) ?: return null
        return if (rune.ring >= 2 && priceFactor != 1.0f) (base * priceFactor).toInt().coerceAtLeast(1) else base
    }
}

/** What the bought runes do. All of these scale a value that already exists; none of them touches match scoring or league points. */
object RuneEffects {
    /** The sum of level x perLevel over every rune of [effect]. */
    fun total(levels: Map<String, Int>, effect: RuneEffect): Int =
        RuneTree.all.filter { it.effect == effect }.sumOf { RuneTree.levelOf(levels, it.id) * it.perLevel }

    /** The ceiling of each effect's total, so no stack of runes can run away (null: no ceiling, a one-off ability). */
    fun capOf(effect: RuneEffect): Int? = when (effect) {
        RuneEffect.WORD -> 40
        RuneEffect.WIN -> 40
        RuneEffect.LOGIN -> 50
        RuneEffect.DAILY -> 50
        RuneEffect.CHEST -> 50
        RuneEffect.CJOKER -> 20
        RuneEffect.DISC -> 40
        RuneEffect.XP -> 30
        RuneEffect.CTIME -> 40
        RuneEffect.ADCD -> 30
        RuneEffect.ROOM -> 40
        RuneEffect.HOST -> 30
        else -> null
    }

    /** [total] held to the effect's ceiling (see [capOf]). */
    private fun capped(levels: Map<String, Int>, effect: RuneEffect): Int = total(levels, effect).coerceAtMost(capOf(effect) ?: Int.MAX_VALUE)

    private fun scaled(base: Int, percent: Int): Int = if (percent <= 0) base else ((base * (100L + percent)) / 100L).toInt()

    /** Gold for a round's correct words, [base] being all of them together (the percent is taken of the whole, so no rounding eats it). */
    fun wordGold(levels: Map<String, Int>, base: Int): Int = scaled(base, capped(levels, RuneEffect.WORD))

    /** Gold for winning an online room. */
    fun winGold(levels: Map<String, Int>, base: Int): Int = scaled(base, capped(levels, RuneEffect.WIN))

    /** Gold of a Giriş Serisi day. */
    fun loginGold(levels: Map<String, Int>, base: Int): Int = scaled(base, capped(levels, RuneEffect.LOGIN))

    /** Gold of the daily challenge. */
    fun dailyGold(levels: Map<String, Int>, base: Int): Int = scaled(base, capped(levels, RuneEffect.DAILY))

    /** Percent added to the gold a chest pays. */
    fun chestGoldPercent(levels: Map<String, Int>): Int = capped(levels, RuneEffect.CHEST)

    /** Percentage points added to every "another joker" roll of a chest. */
    fun chestJokerBonusPercent(levels: Map<String, Int>): Int = capped(levels, RuneEffect.CJOKER)

    /** Percent off a joker's store price, never past 60. */
    fun jokerDiscountPercent(levels: Map<String, Int>): Int = capped(levels, RuneEffect.DISC)

    /** Extra jokers on top of the daily ad joker. */
    fun extraDailyJokers(levels: Map<String, Int>): Int = total(levels, RuneEffect.DJ).coerceIn(0, 10)

    /** Percent more experience toward the player's level (league points are untouched), up to 30. */
    fun xpPercent(levels: Map<String, Int>): Int = capped(levels, RuneEffect.XP)

    /** Percent off the time a chest takes to open, up to 40. */
    fun chestTimeReductionPercent(levels: Map<String, Int>): Int = capped(levels, RuneEffect.CTIME)

    /** Percent off the wait between free ad gifts, up to 30. */
    fun adCooldownReductionPercent(levels: Map<String, Int>): Int = capped(levels, RuneEffect.ADCD)

    /** Percent more gold from an online room, up to 40; [asHost] adds the founder runes on top (up to 30 more). */
    fun roomGoldPercent(levels: Map<String, Int>, asHost: Boolean): Int =
        capped(levels, RuneEffect.ROOM) + if (asHost) capped(levels, RuneEffect.HOST) else 0

    /** A one-off ability: its rune is bought. */
    fun has(levels: Map<String, Int>, effect: RuneEffect): Boolean =
        RuneTree.all.any { it.effect == effect && RuneTree.levelOf(levels, it.id) >= 1 }
}

/**
 * The player's rune levels as they stand now, kept up to date by SettingsRepository, so a screen that only SHOWS an amount
 * (a chest's wait, a round's XP, a login day's gold) can show it with the runes applied, exactly as it will be paid.
 */
object RuneLive {
    @Volatile var levels: Map<String, Int> = emptyMap()

    /** [amount] XP as the level bar receives it (same rounding as SettingsRepository.addXp). */
    fun xp(amount: Int): Int = if (amount <= 0) amount else amount + amount * RuneEffects.xpPercent(levels) / 100

    /** A login day's [gold] as it is paid. */
    fun loginGold(gold: Int): Int = if (gold <= 0) gold else RuneEffects.loginGold(levels, gold)

    /** [millis] of chest waiting with the chest-time rune taken off. */
    fun chestMillis(millis: Long): Long = millis * (100 - RuneEffects.chestTimeReductionPercent(levels)) / 100
}

/**
 * What a style rune puts in the player's collection when it is bought (a pen or frame found only on the rune map).
 * Empty for now: the runes no longer give rewards; the plumbing stays for when they come back.
 */
object RuneRewards {
    val storeItem: Map<String, String> = emptyMap()
}
