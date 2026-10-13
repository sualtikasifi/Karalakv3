package com.sualtikasifi.cizimhafiza.domain.model

import com.sualtikasifi.cizimhafiza.R

/** A branch of the rune map; [ROOT] is the centre every path starts from. */
enum class RuneBranch { ROOT, ECONOMY, CHEST }

/**
 * One node of the rune map ("Karalak Rünleri"): bought with gold, level by level, for a small permanent bonus.
 *
 * [id] is persisted (the owned levels are stored by it) — never rename an existing one. [x]/[y] are the node's place on
 * the map in map units (the centre is 0,0; one unit is a node spacing). A node opens once everything in [requires] has
 * been bought at least once. A rune never touches online match scoring or XP, so the league stays fair.
 */
data class Rune(
    val id: String,
    val branch: RuneBranch,
    val nameRes: Int,
    /** Takes the percentage as %1$d. */
    val descRes: Int,
    val icon: String,
    /** What each level adds, in percent (or percentage points). */
    val percentPerLevel: Int,
    /** Gold for level 1, 2, 3 … — its size is the max level. */
    val costs: List<Int>,
    val requires: List<String>,
    val x: Float,
    val y: Float
) {
    val maxLevel: Int get() = costs.size
}

/** A node that is drawn as a locked silhouette so the map visibly has room to grow; it cannot be bought. */
data class RunePlaceholder(val x: Float, val y: Float, val requires: String)

object RuneTree {
    const val ROOT_ID = "root"
    const val WORD_GOLD = "word_gold"
    const val WIN_GOLD = "win_gold"
    const val LOGIN_GOLD = "login_gold"
    const val CHEST_GOLD = "chest_gold"
    const val CHEST_JOKER = "chest_joker"

    val all: List<Rune> = listOf(
        Rune(ROOT_ID, RuneBranch.ROOT, R.string.rune_root_name, R.string.rune_root_desc, "🐶", 0, listOf(0), emptyList(), 0f, 0f),
        Rune(WORD_GOLD, RuneBranch.ECONOMY, R.string.rune_word_gold_name, R.string.rune_word_gold_desc, "✏️", 10, listOf(300, 600, 1_200), listOf(ROOT_ID), 1f, 0f),
        Rune(WIN_GOLD, RuneBranch.ECONOMY, R.string.rune_win_gold_name, R.string.rune_win_gold_desc, "🏆", 10, listOf(800, 1_600, 3_200), listOf(WORD_GOLD), 2f, -0.8f),
        Rune(LOGIN_GOLD, RuneBranch.ECONOMY, R.string.rune_login_gold_name, R.string.rune_login_gold_desc, "📅", 20, listOf(1_000, 2_000, 4_000), listOf(WORD_GOLD), 2f, 0.8f),
        Rune(CHEST_GOLD, RuneBranch.CHEST, R.string.rune_chest_gold_name, R.string.rune_chest_gold_desc, "🎁", 5, listOf(1_500, 3_000, 6_000), listOf(ROOT_ID), 0f, -1f),
        Rune(CHEST_JOKER, RuneBranch.CHEST, R.string.rune_chest_joker_name, R.string.rune_chest_joker_desc, "🃏", 5, listOf(1_000, 2_000, 4_000), listOf(CHEST_GOLD), -0.9f, -2f)
    )

    /** Where later branches (style, jokers and games) will grow from. */
    val placeholders: List<RunePlaceholder> = listOf(
        RunePlaceholder(0f, 1f, ROOT_ID),
        RunePlaceholder(-1f, 0f, ROOT_ID),
        RunePlaceholder(-1.9f, 0.8f, ROOT_ID),
        RunePlaceholder(0.9f, -2f, CHEST_GOLD),
        RunePlaceholder(3f, 0f, WORD_GOLD)
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
}

/** What the bought runes do. All of these scale a value that already exists; none of them touches match scoring or XP. */
object RuneEffects {
    private fun pct(levels: Map<String, Int>, id: String): Int =
        (RuneTree.get(id)?.percentPerLevel ?: 0) * RuneTree.levelOf(levels, id)

    private fun scaled(base: Int, percent: Int): Int = if (percent <= 0) base else ((base * (100 + percent)) / 100.0).toInt()

    /** Gold for one correct word. */
    fun goldPerWord(levels: Map<String, Int>, base: Int): Int = scaled(base, pct(levels, RuneTree.WORD_GOLD))

    /** Gold for winning an online room. */
    fun winGold(levels: Map<String, Int>, base: Int): Int = scaled(base, pct(levels, RuneTree.WIN_GOLD))

    /** Gold of a Giriş Serisi day. */
    fun loginGold(levels: Map<String, Int>, base: Int): Int = scaled(base, pct(levels, RuneTree.LOGIN_GOLD))

    /** Percent added to the gold a chest pays. */
    fun chestGoldPercent(levels: Map<String, Int>): Int = pct(levels, RuneTree.CHEST_GOLD)

    /** Percentage points added to every "another joker" roll of a chest. */
    fun chestJokerBonusPercent(levels: Map<String, Int>): Int = pct(levels, RuneTree.CHEST_JOKER)
}
