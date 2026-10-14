package com.sualtikasifi.cizimhafiza.domain.model

import com.sualtikasifi.cizimhafiza.R

/** A branch of the rune map; [ROOT] is the centre every path starts from. */
enum class RuneBranch { ROOT, GOLD, CHEST, LUCK, GREEN, BLUE, PURPLE, CYAN, LEGEND }

/** How an effect's number is written: "+%5", "+3 puan", "+1 adet", or just "on" for a one-off ability. */
enum class RuneUnit { PERCENT, POINTS, COUNT, FLAG }

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
    DJ("cards", "🎴", RuneUnit.COUNT, R.string.rune_fx_dj_desc, R.string.rune_fx_dj_label),
    XP("owl", "🦉", RuneUnit.PERCENT, R.string.rune_fx_xp_desc, R.string.rune_fx_xp_label),
    CTIME("stopwatch", "⏱️", RuneUnit.PERCENT, R.string.rune_fx_ctime_desc, R.string.rune_fx_ctime_label),
    ADCD("play", "▶️", RuneUnit.PERCENT, R.string.rune_fx_adcd_desc, R.string.rune_fx_adcd_label),
    ROOM("friends", "👥", RuneUnit.PERCENT, R.string.rune_fx_room_desc, R.string.rune_fx_room_label),
    HOST("house", "🏠", RuneUnit.PERCENT, R.string.rune_fx_host_desc, R.string.rune_fx_host_label),
    LWORD("pencil", "✏️", RuneUnit.PERCENT, R.string.rune_fx_lword_desc, R.string.rune_fx_lword_label),
    LCHEST("chest", "📦", RuneUnit.PERCENT, R.string.rune_fx_lchest_desc, R.string.rune_fx_lchest_label),
    LACH("medal", "🎖️", RuneUnit.PERCENT, R.string.rune_fx_lach_desc, R.string.rune_fx_lach_label),
    LSTAR("star", "⭐", RuneUnit.PERCENT, R.string.rune_fx_lstar_desc, R.string.rune_fx_lstar_label),
    STREAK("chain", "⛓️", RuneUnit.FLAG, R.string.rune_fx_streak_desc, R.string.rune_fx_streak_label),
    INSTANT("key", "🔑", RuneUnit.FLAG, R.string.rune_fx_instant_desc, R.string.rune_fx_instant_label),
    DICE("dice", "🎲", RuneUnit.FLAG, R.string.rune_fx_dice_desc, R.string.rune_fx_dice_label),
    RESERVE("slot", "🗃️", RuneUnit.FLAG, R.string.rune_fx_reserve_desc, R.string.rune_fx_reserve_label),
    BEST("bond", "💞", RuneUnit.FLAG, R.string.rune_fx_best_desc, R.string.rune_fx_best_label),
    INSURE("shield", "🛡️", RuneUnit.FLAG, R.string.rune_fx_insure_desc, R.string.rune_fx_insure_label),
    STYLE("brush", "🖌️", RuneUnit.FLAG, R.string.rune_fx_style_desc, R.string.rune_fx_style_label);
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
        Rune("disc_4", RuneBranch.LUCK, RuneEffect.DISC, R.string.rune_disc_4_name, 1, listOf(400, 800, 1_600), listOf("ach_1"), 2.040f, 0.824f),
        Rune("word_4", RuneBranch.GOLD, RuneEffect.WORD, R.string.rune_word_4_name, 2, listOf(2_750, 5_500, 11_000), listOf("word_key"), 0.000f, -7.900f, 2),
        Rune("login_4", RuneBranch.GOLD, RuneEffect.LOGIN, R.string.rune_login_4_name, 2, listOf(2_750, 5_500, 11_000), listOf("word_4"), -1.150f, -8.850f, 2),
        Rune("daily_4", RuneBranch.GOLD, RuneEffect.DAILY, R.string.rune_daily_4_name, 2, listOf(3_000, 6_000, 12_000), listOf("word_4"), 1.150f, -8.850f, 2),
        Rune("streak_key", RuneBranch.GOLD, RuneEffect.STREAK, R.string.rune_streak_key_name, 0, listOf(37_250), listOf("login_4", "daily_4"), 0.000f, -9.800f, 2, true),
        Rune("chest_6", RuneBranch.CHEST, RuneEffect.CHEST, R.string.rune_chest_6_name, 2, listOf(3_000, 6_000, 12_000), listOf("star_4"), -7.100f, 0.100f, 2),
        Rune("star_5", RuneBranch.CHEST, RuneEffect.STAR, R.string.rune_star_5_name, 2, listOf(3_000, 6_000, 12_000), listOf("chest_6"), -8.100f, -0.850f, 2),
        Rune("cjoker_4", RuneBranch.CHEST, RuneEffect.CJOKER, R.string.rune_cjoker_4_name, 2, listOf(3_000, 6_000, 12_000), listOf("chest_6"), -8.100f, 1.050f, 2),
        Rune("chest_key2", RuneBranch.CHEST, RuneEffect.INSTANT, R.string.rune_chest_key2_name, 0, listOf(37_250), listOf("star_5", "cjoker_4"), -9.100f, 0.100f, 2, true),
        Rune("ach_5", RuneBranch.LUCK, RuneEffect.ACH, R.string.rune_ach_5_name, 2, listOf(2_750, 5_500, 11_000), listOf("dj_key"), 4.900f, 4.900f, 2),
        Rune("disc_5", RuneBranch.LUCK, RuneEffect.DISC, R.string.rune_disc_5_name, 1, listOf(3_000, 6_000, 12_000), listOf("ach_5"), 5.700f, 4.100f, 2),
        Rune("ach_6", RuneBranch.LUCK, RuneEffect.ACH, R.string.rune_ach_6_name, 2, listOf(3_000, 6_000, 12_000), listOf("ach_5"), 4.100f, 5.700f, 2),
        Rune("luck_key", RuneBranch.LUCK, RuneEffect.DICE, R.string.rune_luck_key_name, 0, listOf(37_250), listOf("disc_5", "ach_6"), 5.700f, 5.700f, 2, true),
        Rune("time_1", RuneBranch.GREEN, RuneEffect.CTIME, R.string.rune_time_1_name, 5, listOf(3_000, 6_000, 12_000), listOf("chest_5"), -3.000f, 5.000f, 2),
        Rune("time_2", RuneBranch.GREEN, RuneEffect.CTIME, R.string.rune_time_2_name, 5, listOf(3_500, 7_000, 14_000), listOf("time_1"), -4.100f, 5.700f, 2),
        Rune("ad_1", RuneBranch.GREEN, RuneEffect.ADCD, R.string.rune_ad_1_name, 5, listOf(3_500, 7_000, 14_000), listOf("time_1"), -1.900f, 5.700f, 2),
        Rune("time_3", RuneBranch.GREEN, RuneEffect.CTIME, R.string.rune_time_3_name, 5, listOf(3_750, 7_500, 15_000), listOf("time_2"), -4.100f, 6.850f, 2),
        Rune("ad_2", RuneBranch.GREEN, RuneEffect.ADCD, R.string.rune_ad_2_name, 5, listOf(3_750, 7_500, 15_000), listOf("ad_1"), -1.900f, 6.850f, 2),
        Rune("slot_key", RuneBranch.GREEN, RuneEffect.RESERVE, R.string.rune_slot_key_name, 0, listOf(49_500), listOf("time_3", "ad_2"), -3.000f, 7.900f, 2, true),
        Rune("fr_1", RuneBranch.BLUE, RuneEffect.ROOM, R.string.rune_fr_1_name, 4, listOf(3_000, 6_000, 12_000), listOf("ach_3"), 4.600f, 1.100f, 2),
        Rune("fr_2", RuneBranch.BLUE, RuneEffect.HOST, R.string.rune_fr_2_name, 5, listOf(3_500, 7_000, 14_000), listOf("fr_1"), 5.700f, 0.300f, 2),
        Rune("fr_3", RuneBranch.BLUE, RuneEffect.ROOM, R.string.rune_fr_3_name, 4, listOf(3_750, 7_500, 15_000), listOf("fr_1"), 5.700f, 1.900f, 2),
        Rune("fr_4", RuneBranch.BLUE, RuneEffect.HOST, R.string.rune_fr_4_name, 5, listOf(3_750, 7_500, 15_000), listOf("fr_2"), 6.800f, 0.300f, 2),
        Rune("fr_5", RuneBranch.BLUE, RuneEffect.ROOM, R.string.rune_fr_5_name, 4, listOf(4_000, 8_000, 16_000), listOf("fr_3"), 6.800f, 1.900f, 2),
        Rune("fr_key", RuneBranch.BLUE, RuneEffect.BEST, R.string.rune_fr_key_name, 0, listOf(49_500), listOf("fr_4", "fr_5"), 7.900f, 1.100f, 2, true),
        Rune("st_1", RuneBranch.PURPLE, RuneEffect.STYLE, R.string.rune_st_1_name, 0, listOf(25_750), listOf("streak_key"), 0.000f, -10.900f, 3, false),
        Rune("st_2", RuneBranch.PURPLE, RuneEffect.STYLE, R.string.rune_st_2_name, 0, listOf(25_750), listOf("st_1"), -1.150f, -11.850f, 3, false),
        Rune("st_3", RuneBranch.PURPLE, RuneEffect.STYLE, R.string.rune_st_3_name, 0, listOf(30_000), listOf("st_1"), 1.150f, -11.850f, 3, false),
        Rune("st_4", RuneBranch.PURPLE, RuneEffect.STYLE, R.string.rune_st_4_name, 0, listOf(30_000), listOf("st_2"), -1.150f, -13.000f, 3, false),
        Rune("st_5", RuneBranch.PURPLE, RuneEffect.STYLE, R.string.rune_st_5_name, 0, listOf(30_000), listOf("st_3"), 1.150f, -13.000f, 3, false),
        Rune("st_6", RuneBranch.PURPLE, RuneEffect.STYLE, R.string.rune_st_6_name, 0, listOf(34_500), listOf("st_4"), -1.150f, -14.150f, 3, false),
        Rune("st_7", RuneBranch.PURPLE, RuneEffect.STYLE, R.string.rune_st_7_name, 0, listOf(34_500), listOf("st_5"), 1.150f, -14.150f, 3, false),
        Rune("st_key", RuneBranch.PURPLE, RuneEffect.STYLE, R.string.rune_st_key_name, 0, listOf(129_000), listOf("st_6", "st_7"), 0.000f, -15.200f, 3, true),
        Rune("xp_1", RuneBranch.CYAN, RuneEffect.XP, R.string.rune_xp_1_name, 3, listOf(6_000, 12_000, 24_000), listOf("luck_key"), 6.500f, 6.500f, 3),
        Rune("xp_2", RuneBranch.CYAN, RuneEffect.XP, R.string.rune_xp_2_name, 3, listOf(6_500, 13_000, 26_000), listOf("xp_1"), 7.300f, 5.700f, 3),
        Rune("xp_3", RuneBranch.CYAN, RuneEffect.XP, R.string.rune_xp_3_name, 3, listOf(7_000, 14_000, 28_000), listOf("xp_1"), 5.700f, 7.300f, 3),
        Rune("xp_4", RuneBranch.CYAN, RuneEffect.XP, R.string.rune_xp_4_name, 3, listOf(7_250, 14_500, 29_000), listOf("xp_2"), 8.100f, 6.500f, 3),
        Rune("xp_5", RuneBranch.CYAN, RuneEffect.XP, R.string.rune_xp_5_name, 3, listOf(7_750, 15_500, 31_000), listOf("xp_3"), 6.500f, 8.100f, 3),
        Rune("ins_key", RuneBranch.CYAN, RuneEffect.INSURE, R.string.rune_ins_key_name, 0, listOf(77_500), listOf("xp_4", "xp_5"), 7.300f, 7.300f, 3, true),
        Rune("word_5", RuneBranch.GOLD, RuneEffect.WORD, R.string.rune_word_5_name, 1, listOf(7_750, 15_500, 31_000), listOf("login_4"), -2.300f, -9.600f, 3),
        Rune("login_5", RuneBranch.GOLD, RuneEffect.LOGIN, R.string.rune_login_5_name, 1, listOf(7_750, 15_500, 31_000), listOf("daily_4"), 2.300f, -9.600f, 3),
        Rune("win_4", RuneBranch.GOLD, RuneEffect.WIN, R.string.rune_win_4_name, 1, listOf(8_250, 16_500, 33_000), listOf("win_3"), 2.500f, -6.900f, 3),
        Rune("chest_7", RuneBranch.CHEST, RuneEffect.CHEST, R.string.rune_chest_7_name, 1, listOf(8_250, 16_500, 33_000), listOf("star_5"), -9.300f, -1.750f, 3),
        Rune("star_6", RuneBranch.CHEST, RuneEffect.STAR, R.string.rune_star_6_name, 1, listOf(8_500, 17_000, 34_000), listOf("cjoker_4"), -9.300f, 1.950f, 3),
        Rune("ach_7", RuneBranch.LUCK, RuneEffect.ACH, R.string.rune_ach_7_name, 1, listOf(8_500, 17_000, 34_000), listOf("disc_5"), 6.900f, 3.400f, 3),
        Rune("eg_1", RuneBranch.LEGEND, RuneEffect.LWORD, R.string.rune_eg_1_name, 1, listOf(20_000, 23_000, 26_500, 30_500, 35_000, 40_250, 46_250, 53_250, 61_250, 70_250, 81_000, 93_000, 107_000, 123_000, 141_500, 162_750, 187_250, 215_250, 247_500, 284_750), listOf("word_5"), -2.300f, -10.750f, 4),
        Rune("eg_2", RuneBranch.LEGEND, RuneEffect.LCHEST, R.string.rune_eg_2_name, 1, listOf(20_000, 23_000, 26_500, 30_500, 35_000, 40_250, 46_250, 53_250, 61_250, 70_250, 81_000, 93_000, 107_000, 123_000, 141_500, 162_750, 187_250, 215_250, 247_500, 284_750), listOf("chest_7"), -10.400f, -2.600f, 4),
        Rune("eg_3", RuneBranch.LEGEND, RuneEffect.LACH, R.string.rune_eg_3_name, 1, listOf(20_000, 23_000, 26_500, 30_500, 35_000, 40_250, 46_250, 53_250, 61_250, 70_250, 81_000, 93_000, 107_000, 123_000, 141_500, 162_750, 187_250, 215_250, 247_500, 284_750), listOf("ach_7"), 7.900f, 2.600f, 4),
        Rune("eg_4", RuneBranch.LEGEND, RuneEffect.LSTAR, R.string.rune_eg_4_name, 1, listOf(20_000, 23_000, 26_500, 30_500, 35_000, 40_250, 46_250, 53_250, 61_250, 70_250, 81_000, 93_000, 107_000, 123_000, 141_500, 162_750, 187_250, 215_250, 247_500, 284_750), listOf("star_6"), -10.400f, 2.800f, 4)
    )


    private val byId = all.associateBy { it.id }

    fun get(id: String): Rune? = byId[id]

    /** Level the player holds; the root is always held. */
    fun levelOf(levels: Map<String, Int>, id: String): Int = if (id == ROOT_ID) 1 else levels[id] ?: 0

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
    val totalCost: Int get() = all.sumOf { it.costs.sum() }
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

    /** [total] held to a ceiling, so no stack of runes can run away. */
    private fun capped(levels: Map<String, Int>, effect: RuneEffect, cap: Int): Int = total(levels, effect).coerceAtMost(cap)

    private fun scaled(base: Int, percent: Int): Int = if (percent <= 0) base else ((base * (100L + percent)) / 100L).toInt()

    /** Gold for one correct word: the map's word runes (up to +60%) and the Legend pool (up to +20%). */
    fun goldPerWord(levels: Map<String, Int>, base: Int): Int =
        scaled(base, capped(levels, RuneEffect.WORD, 60) + capped(levels, RuneEffect.LWORD, 20))

    /** Gold for winning an online room. */
    fun winGold(levels: Map<String, Int>, base: Int): Int = scaled(base, capped(levels, RuneEffect.WIN, 50))

    /** Gold of a Giriş Serisi day. */
    fun loginGold(levels: Map<String, Int>, base: Int): Int = scaled(base, capped(levels, RuneEffect.LOGIN, 75))

    /** Gold of the daily challenge. */
    fun dailyGold(levels: Map<String, Int>, base: Int): Int = scaled(base, capped(levels, RuneEffect.DAILY, 50))

    /** Gold of one achievement. */
    fun achievementGold(levels: Map<String, Int>, base: Int): Int =
        scaled(base, capped(levels, RuneEffect.ACH, 80) + capped(levels, RuneEffect.LACH, 20))

    /** Gold of one new level star. */
    fun starGold(levels: Map<String, Int>, base: Int): Int =
        scaled(base, capped(levels, RuneEffect.STAR, 70) + capped(levels, RuneEffect.LSTAR, 20))

    /** Percent added to the gold a chest pays. */
    fun chestGoldPercent(levels: Map<String, Int>): Int = capped(levels, RuneEffect.CHEST, 60) + capped(levels, RuneEffect.LCHEST, 20)

    /** Percentage points added to every "another joker" roll of a chest. */
    fun chestJokerBonusPercent(levels: Map<String, Int>): Int = capped(levels, RuneEffect.CJOKER, 30)

    /** Percent off a joker's store price, never past 60. */
    fun jokerDiscountPercent(levels: Map<String, Int>): Int = total(levels, RuneEffect.DISC).coerceIn(0, 60)

    /** Extra jokers on top of the daily ad joker. */
    fun extraDailyJokers(levels: Map<String, Int>): Int = total(levels, RuneEffect.DJ)

    /** Percent more experience toward the player's level (league points are untouched), up to 45. */
    fun xpPercent(levels: Map<String, Int>): Int = capped(levels, RuneEffect.XP, 45)

    /** Percent off the time a chest takes to open, up to 30. */
    fun chestTimeReductionPercent(levels: Map<String, Int>): Int = capped(levels, RuneEffect.CTIME, 30)

    /** Percent off the wait between free ad gifts, up to 30. */
    fun adCooldownReductionPercent(levels: Map<String, Int>): Int = capped(levels, RuneEffect.ADCD, 30)

    /** Percent more gold from an online room, up to 40; [asHost] adds the founder runes on top (up to 30 more). */
    fun roomGoldPercent(levels: Map<String, Int>, asHost: Boolean): Int =
        capped(levels, RuneEffect.ROOM, 40) + if (asHost) capped(levels, RuneEffect.HOST, 30) else 0

    /** A one-off ability: its rune is bought. */
    fun has(levels: Map<String, Int>, effect: RuneEffect): Boolean =
        RuneTree.all.any { it.effect == effect && RuneTree.levelOf(levels, it.id) >= 1 }
}

/** What a style rune puts in the player's collection when it is bought (a store pen or frame id, see StoreViewModel). */
object RuneRewards {
    val storeItem: Map<String, String> = mapOf(
        "st_1" to "pen:SUNRISE",
        "st_2" to "pen:CANDY",
        "st_3" to "pen:DIAMOND",
        "st_4" to "frame:CHERRY_BLOSSOM_GOLD",
        "st_5" to "pen:NIGHT",
        "st_6" to "frame:TIME_EXPLORER",
        "st_7" to "frame:OCEAN_TREASURE",
        "st_key" to "frame:ROYAL_CROWN"
    )
}
