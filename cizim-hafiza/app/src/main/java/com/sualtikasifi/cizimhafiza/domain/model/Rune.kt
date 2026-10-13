package com.sualtikasifi.cizimhafiza.domain.model

import com.sualtikasifi.cizimhafiza.R

/** A branch of the rune map; [ROOT] is the centre every path starts from. */
enum class RuneBranch { ROOT, GOLD, CHEST, LUCK }

/** How an effect's number is written: "+%5", "+3 puan", "+1 adet". */
enum class RuneUnit { PERCENT, POINTS, COUNT }

/**
 * What a rune does. Many runes share one effect and their levels add up, so a long map needs only a handful of
 * real effects. [iconKey] names the icon (a drawable called rune_icon_<iconKey> when present, otherwise [emoji]).
 * None of these touches match scoring or XP, so the league stays fair.
 */
enum class RuneEffect(val iconKey: String, val emoji: String, val unit: RuneUnit, val descRes: Int, val labelRes: Int) {
    NONE("dog", "🐶", RuneUnit.PERCENT, R.string.rune_root_desc, R.string.rune_root_desc),
    WORD("pencil", "✏️", RuneUnit.PERCENT, R.string.rune_fx_word_desc, R.string.rune_fx_word_label),
    WIN("trophy", "🏆", RuneUnit.PERCENT, R.string.rune_fx_win_desc, R.string.rune_fx_win_label),
    LOGIN("gift", "🎁", RuneUnit.PERCENT, R.string.rune_fx_login_desc, R.string.rune_fx_login_label),
    DAILY("calendar", "📅", RuneUnit.PERCENT, R.string.rune_fx_daily_desc, R.string.rune_fx_daily_label),
    ACH("medal", "🎖️", RuneUnit.PERCENT, R.string.rune_fx_ach_desc, R.string.rune_fx_ach_label),
    STAR("star", "⭐", RuneUnit.PERCENT, R.string.rune_fx_star_desc, R.string.rune_fx_star_label),
    CHEST("chest", "📦", RuneUnit.PERCENT, R.string.rune_fx_chest_desc, R.string.rune_fx_chest_label),
    CJOKER("joker", "🃏", RuneUnit.POINTS, R.string.rune_fx_cjoker_desc, R.string.rune_fx_cjoker_label),
    DISC("tag", "🏷️", RuneUnit.PERCENT, R.string.rune_fx_disc_desc, R.string.rune_fx_disc_label),
    DJ("cards", "🎴", RuneUnit.COUNT, R.string.rune_fx_dj_desc, R.string.rune_fx_dj_label);
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
    val y: Float
) {
    val maxLevel: Int get() = costs.size
    val isKeystone: Boolean get() = costs.size == 1
}

/** A node drawn as a locked silhouette so the map visibly has room to grow; [parent] is the index of the one before it (null: the centre). */
data class RunePlaceholder(val x: Float, val y: Float, val parent: Int?)

object RuneTree {
    const val ROOT_ID = "root"
    const val WORD_GOLD = "word_gold"
    const val WIN_GOLD = "win_gold"
    const val LOGIN_GOLD = "login_gold"
    const val CHEST_GOLD = "chest_gold"
    const val CHEST_JOKER = "chest_joker"

    val all: List<Rune> = listOf(
        Rune(ROOT_ID, RuneBranch.ROOT, RuneEffect.NONE, R.string.rune_root_name, 0, listOf(0), emptyList(), 0f, 0f),
        Rune("word_gold", RuneBranch.GOLD, RuneEffect.WORD, R.string.rune_word_gold_name, 3, listOf(200, 400, 800), listOf(ROOT_ID), 0.000f, -1.000f),
        Rune("login_gold", RuneBranch.GOLD, RuneEffect.LOGIN, R.string.rune_login_gold_name, 6, listOf(400, 800, 1_600), listOf("word_gold"), 0.000f, -2.000f),
        Rune("daily_1", RuneBranch.GOLD, RuneEffect.DAILY, R.string.rune_daily_1_name, 4, listOf(700, 1_400, 2_800), listOf("login_gold"), 0.000f, -3.000f),
        Rune("word_2", RuneBranch.GOLD, RuneEffect.WORD, R.string.rune_word_2_name, 3, listOf(700, 1_400, 2_800), listOf("login_gold"), -1.325f, -2.120f),
        Rune("win_gold", RuneBranch.GOLD, RuneEffect.WIN, R.string.rune_win_gold_name, 4, listOf(1_350, 2_700, 5_400), listOf("word_2"), -2.216f, -2.837f),
        Rune("login_2", RuneBranch.GOLD, RuneEffect.LOGIN, R.string.rune_login_2_name, 7, listOf(700, 1_400, 2_800), listOf("login_gold"), 1.539f, -1.970f),
        Rune("daily_2", RuneBranch.GOLD, RuneEffect.DAILY, R.string.rune_daily_2_name, 5, listOf(1_350, 2_700, 5_400), listOf("login_2"), 2.758f, -2.314f),
        Rune("word_3", RuneBranch.GOLD, RuneEffect.WORD, R.string.rune_word_3_name, 5, listOf(1_350, 2_700, 5_400), listOf("daily_1"), 0.000f, -4.100f),
        Rune("win_2", RuneBranch.GOLD, RuneEffect.WIN, R.string.rune_win_2_name, 5, listOf(2_600, 5_200, 10_400), listOf("word_3"), -2.300f, -4.326f),
        Rune("login_3", RuneBranch.GOLD, RuneEffect.LOGIN, R.string.rune_login_3_name, 7, listOf(2_600, 5_200, 10_400), listOf("word_3"), 2.300f, -4.326f),
        Rune("daily_3", RuneBranch.GOLD, RuneEffect.DAILY, R.string.rune_daily_3_name, 4, listOf(4_950, 9_900, 19_800), listOf("win_2"), -1.227f, -5.771f),
        Rune("win_3", RuneBranch.GOLD, RuneEffect.WIN, R.string.rune_win_3_name, 4, listOf(4_950, 9_900, 19_800), listOf("login_3"), 1.227f, -5.771f),
        Rune("word_key", RuneBranch.GOLD, RuneEffect.WORD, R.string.rune_word_key_name, 4, listOf(75_200), listOf("daily_3", "win_3"), 0.000f, -6.800f),
        Rune("word_box", RuneBranch.GOLD, RuneEffect.WORD, R.string.rune_word_box_name, 2, listOf(400, 800, 1_600), listOf("word_gold"), 1.340f, -1.047f),
        Rune("chest_gold", RuneBranch.CHEST, RuneEffect.CHEST, R.string.rune_chest_gold_name, 3, listOf(200, 400, 800), listOf(ROOT_ID), -1.000f, 0.000f),
        Rune("star_1", RuneBranch.CHEST, RuneEffect.STAR, R.string.rune_star_1_name, 5, listOf(400, 800, 1_600), listOf("chest_gold"), -2.000f, 0.000f),
        Rune("chest_joker", RuneBranch.CHEST, RuneEffect.CJOKER, R.string.rune_chest_joker_name, 2, listOf(700, 1_400, 2_800), listOf("star_1"), -3.100f, 0.000f),
        Rune("chest_2", RuneBranch.CHEST, RuneEffect.CHEST, R.string.rune_chest_2_name, 3, listOf(700, 1_400, 2_800), listOf("star_1"), -2.503f, 1.011f),
        Rune("star_2", RuneBranch.CHEST, RuneEffect.STAR, R.string.rune_star_2_name, 5, listOf(1_350, 2_700, 5_400), listOf("chest_2"), -2.988f, 2.507f),
        Rune("cjoker_2", RuneBranch.CHEST, RuneEffect.CJOKER, R.string.rune_cjoker_2_name, 2, listOf(700, 1_400, 2_800), listOf("star_1"), -2.669f, -1.133f),
        Rune("chest_3", RuneBranch.CHEST, RuneEffect.CHEST, R.string.rune_chest_3_name, 4, listOf(1_350, 2_700, 5_400), listOf("cjoker_2"), -3.277f, -2.294f),
        Rune("star_3", RuneBranch.CHEST, RuneEffect.STAR, R.string.rune_star_3_name, 4, listOf(1_350, 2_700, 5_400), listOf("chest_joker"), -4.300f, 0.000f),
        Rune("cjoker_3", RuneBranch.CHEST, RuneEffect.CJOKER, R.string.rune_cjoker_3_name, 1, listOf(2_600, 5_200, 10_400), listOf("star_3"), -4.886f, -1.779f),
        Rune("chest_4", RuneBranch.CHEST, RuneEffect.CHEST, R.string.rune_chest_4_name, 3, listOf(2_600, 5_200, 10_400), listOf("star_3"), -4.698f, 1.710f),
        Rune("star_4", RuneBranch.CHEST, RuneEffect.STAR, R.string.rune_star_4_name, 3, listOf(2_600, 5_200, 10_400), listOf("star_3"), -6.000f, 0.000f),
        Rune("cjoker_key", RuneBranch.CHEST, RuneEffect.CJOKER, R.string.rune_cjoker_key_name, 3, listOf(39_600), listOf("cjoker_3", "chest_4"), -6.401f, -1.129f),
        Rune("chest_5", RuneBranch.CHEST, RuneEffect.CHEST, R.string.rune_chest_5_name, 1, listOf(2_600, 5_200, 10_400), listOf("star_2"), -3.017f, 3.861f),
        Rune("ach_1", RuneBranch.LUCK, RuneEffect.ACH, R.string.rune_ach_1_name, 5, listOf(200, 400, 800), listOf(ROOT_ID), 0.643f, 0.766f),
        Rune("disc_1", RuneBranch.LUCK, RuneEffect.DISC, R.string.rune_disc_1_name, 4, listOf(400, 800, 1_600), listOf("ach_1"), 1.286f, 1.532f),
        Rune("ach_2", RuneBranch.LUCK, RuneEffect.ACH, R.string.rune_ach_2_name, 5, listOf(700, 1_400, 2_800), listOf("disc_1"), 1.928f, 2.298f),
        Rune("disc_2", RuneBranch.LUCK, RuneEffect.DISC, R.string.rune_disc_2_name, 4, listOf(700, 1_400, 2_800), listOf("disc_1"), 0.235f, 2.690f),
        Rune("dj_1", RuneBranch.LUCK, RuneEffect.DJ, R.string.rune_dj_1_name, 1, listOf(1_350, 2_700, 5_400), listOf("disc_2"), 0.136f, 3.898f),
        Rune("ach_3", RuneBranch.LUCK, RuneEffect.ACH, R.string.rune_ach_3_name, 4, listOf(1_350, 2_700, 5_400), listOf("ach_2"), 3.424f, 1.112f),
        Rune("disc_3", RuneBranch.LUCK, RuneEffect.DISC, R.string.rune_disc_3_name, 3, listOf(1_350, 2_700, 5_400), listOf("ach_2"), 2.160f, 4.062f),
        Rune("ach_4", RuneBranch.LUCK, RuneEffect.ACH, R.string.rune_ach_4_name, 4, listOf(2_600, 5_200, 10_400), listOf("ach_3"), 4.062f, 2.160f),
        Rune("dj_key", RuneBranch.LUCK, RuneEffect.DJ, R.string.rune_dj_key_name, 1, listOf(20_800), listOf("disc_3", "ach_4"), 4.101f, 4.101f),
        Rune("disc_4", RuneBranch.LUCK, RuneEffect.DISC, R.string.rune_disc_4_name, 1, listOf(400, 800, 1_600), listOf("ach_1"), 2.040f, 0.824f)
    )

    /** Where later branches (speed, friends, style, time) will grow from. */
    val placeholders: List<RunePlaceholder> = listOf(
        RunePlaceholder(1.089f, -0.153f, null),
        RunePlaceholder(2.179f, -0.306f, 0),
        RunePlaceholder(-0.450f, 1.113f, null),
        RunePlaceholder(-0.862f, 2.133f, 2)
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): Rune? = byId[id]

    /** Level the player holds; the root is always held. */
    fun levelOf(levels: Map<String, Int>, id: String): Int = if (id == ROOT_ID) 1 else levels[id] ?: 0

    fun isUnlocked(levels: Map<String, Int>, rune: Rune): Boolean = rune.requires.all { levelOf(levels, it) >= 1 }

    /** Gold for the next level, or null at the max (and for the root). */
    fun nextCost(levels: Map<String, Int>, rune: Rune): Int? {
        if (rune.id == ROOT_ID) return null
        val level = levelOf(levels, rune.id)
        return rune.costs.getOrNull(level)
    }

    /** True when something can be bought right now with [gold]. */
    fun anyAffordable(levels: Map<String, Int>, gold: Int): Boolean =
        all.any { r -> isUnlocked(levels, r) && (nextCost(levels, r) ?: Int.MAX_VALUE) <= gold }

    /** Total gold of the whole map. */
    val totalCost: Int get() = all.sumOf { it.costs.sum() }
}

/** What the bought runes do. All of these scale a value that already exists; none of them touches match scoring or XP. */
object RuneEffects {
    /** The sum of level x perLevel over every rune of [effect]. */
    fun total(levels: Map<String, Int>, effect: RuneEffect): Int =
        RuneTree.all.filter { it.effect == effect }.sumOf { RuneTree.levelOf(levels, it.id) * it.perLevel }

    private fun scaled(base: Int, percent: Int): Int = if (percent <= 0) base else ((base * (100L + percent)) / 100L).toInt()

    /** Gold for one correct word. */
    fun goldPerWord(levels: Map<String, Int>, base: Int): Int = scaled(base, total(levels, RuneEffect.WORD))

    /** Gold for winning an online room. */
    fun winGold(levels: Map<String, Int>, base: Int): Int = scaled(base, total(levels, RuneEffect.WIN))

    /** Gold of a Giriş Serisi day. */
    fun loginGold(levels: Map<String, Int>, base: Int): Int = scaled(base, total(levels, RuneEffect.LOGIN))

    /** Gold of the daily challenge. */
    fun dailyGold(levels: Map<String, Int>, base: Int): Int = scaled(base, total(levels, RuneEffect.DAILY))

    /** Gold of one achievement. */
    fun achievementGold(levels: Map<String, Int>, base: Int): Int = scaled(base, total(levels, RuneEffect.ACH))

    /** Gold of one new level star. */
    fun starGold(levels: Map<String, Int>, base: Int): Int = scaled(base, total(levels, RuneEffect.STAR))

    /** Percent added to the gold a chest pays. */
    fun chestGoldPercent(levels: Map<String, Int>): Int = total(levels, RuneEffect.CHEST)

    /** Percentage points added to every "another joker" roll of a chest. */
    fun chestJokerBonusPercent(levels: Map<String, Int>): Int = total(levels, RuneEffect.CJOKER)

    /** Percent off a joker's store price, never past 60. */
    fun jokerDiscountPercent(levels: Map<String, Int>): Int = total(levels, RuneEffect.DISC).coerceIn(0, 60)

    /** Extra jokers on top of the daily ad joker. */
    fun extraDailyJokers(levels: Map<String, Int>): Int = total(levels, RuneEffect.DJ)
}
