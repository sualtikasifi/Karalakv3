package com.sualtikasifi.cizimhafiza.domain.model

import androidx.annotation.StringRes
import com.sualtikasifi.cizimhafiza.R

/**
 * A cosmetic pen the player draws with — the second unlockable track
 * alongside [AvatarFrame].
 *
 * The frame ladder alone gives eleven rewards spread across a hundred
 * levels, so most of the climb passes with nothing to show for it. Pens fill
 * that gap: they unlock on the *odd* fives (5, 15, 25 …) precisely so they
 * never land on the same level as a frame, turning one reward every ten
 * levels into one every five.
 *
 * Unlike a frame, a pen is visible in the thing the player is actually
 * doing — their own strokes — and it is visible to opponents too, because
 * the drawing is what the other player has to guess (see
 * OnlinePlayer.penSkinId).
 *
 * The `name` of each constant is persisted (SettingsRepository's selected
 * pen, and the online player entry), so **never rename one** — a rename
 * silently resets every player wearing it. AvatarFrameTest's equivalent
 * guard applies here too, see PenSkinTest.
 */
/**
 * The living paint of a premium pen, drawn by PenBrush (drawPenStroke). Every effect is a pure function of the drawn
 * distance and a clock, so it looks the same on the drawing screen, in an opponent's replay and in the store preview.
 * A pen with any effect but [NONE] is drawn with layered, slightly thicker strokes (see [isPremium]).
 */
enum class PenEffect {
    NONE,
    /** Gold: a bright glint travelling along the stroke and twinkling stars beside it. */
    GLINT,
    /** Rainbow: the colours flow along the stroke. */
    FLOW,
    /** Aurora: slow flowing colours with a soft breathing glow — a light, particle-free effect. */
    AURORA,
    /** Flame: tiny, dense sparks thrown off the stroke that rise and die out. */
    EMBERS,
    /** Storm: short lightning cracks that flash along the stroke. */
    BOLTS,
    /** Cosmos: twinkling stars and a slowly drifting colour. */
    STARS,
    /** Specter: purple smoke curling up from the stroke. */
    SMOKE,
    /** Lion King: a golden glint, rising gold embers and a few stars. */
    ROYAL;

    val isPremium: Boolean get() = this != NONE
}

enum class PenSkin(
    @StringRes val labelRes: Int,
    /** ARGB colours the stroke is drawn with. One entry paints flat; more blend along the stroke. */
    val colors: List<Long>,
    val unlockLevel: Int,
    /**
     * A weekly-league prize rather than a rung on the level ladder.
     *
     * These are excluded from [unlockedFor], so no amount of levelling
     * offers one, and the picker only shows them to a player who actually
     * earned one (SettingsRepository.earnedLeagueRewardIds).
     *
     * [resolve] deliberately does NOT check ownership. It is called for
     * OTHER players' pens too, arriving from their device over Firestore,
     * and this device has no way to know what somebody else won — refusing
     * on that basis would silently repaint every league winner's strokes
     * back to the default in everyone else's game. The trade-off is that a
     * player who edits their own preferences file can wear one unearned;
     * that is cosmetic, invisible to scoring, and cheaper to accept than a
     * server round-trip per opponent.
     */
    val isLeagueReward: Boolean = false,
    /** Gold price in the store; 0 means not sold there. Store pens have unlockLevel 0 like league prizes so [resolve] can render them on whoever wears one. */
    val storePrice: Int = 0,
    /** Found only on the rune map (RuneRewards): owned like a store item, but never sold in the store or dropped by a chest. */
    val isRuneReward: Boolean = false,
    /** Extra paint on the stroke (glow, sparkles) — drawn by PenBrush's drawPenStroke, so opponents see it too. */
    val effect: PenEffect = PenEffect.NONE,
    /**
     * Withdrawn: no longer sold, no longer handed out, and hidden from the picker unless the player already owns it
     * (see [AvatarFrame.retired]). The constant stays — it is persisted by name and an opponent may still wear it.
     */
    val retired: Boolean = false
) {
    CLASSIC(R.string.pen_classic, listOf(0xFF2E2A26), 1),
    // The enum constant keeps its name — it is persisted, and renaming it
    // would reset every player wearing it (see the class doc). Only the
    // colour and label changed: at 0xFF4A4A4A this was a near-black barely
    // a shade off CLASSIC, so the first pen the game ever hands out looked
    // like no reward at all. A true mid-grey reads as a different pencil.
    CHARCOAL(R.string.pen_charcoal, listOf(0xFF8A9199), 5),
    OCEAN(R.string.pen_ocean, listOf(0xFF1B7A8C), 15),
    SUNSET(R.string.pen_sunset, listOf(0xFFEB7A3C, 0xFFE24B6A), 25),
    FOREST(R.string.pen_forest, listOf(0xFF2E7D4F), 35),
    BERRY(R.string.pen_berry, listOf(0xFF8E3A8E), 45),
    GOLD(R.string.pen_gold, listOf(0xFFD4A02A, 0xFFF2D06B), 55),
    NEON(R.string.pen_neon, listOf(0xFF00E5A0, 0xFF00B8D4), 65),
    LAVA(R.string.pen_lava, listOf(0xFFFF3D00, 0xFFFFC107), 75),
    GALAXY(R.string.pen_galaxy, listOf(0xFF5B3FA8, 0xFF9B5DE5, 0xFF3FA8D4), 85),
    RAINBOW(R.string.pen_rainbow, listOf(0xFFE24B4B, 0xFFEB9A3C, 0xFF3FA85E, 0xFF3F7AD4, 0xFF8E3A8E), 95),

    // --- Weekly league prizes (see LeagueReward) ---
    //
    // unlockLevel 0 so resolve() renders them for anybody already wearing
    // one; isLeagueReward is what actually keeps them off the level ladder.
    // Deliberately louder than the level pens: the whole point of a prize
    // nobody can grind for is that it is recognisable in somebody else's
    // drawing.
    LEAGUE_AURORA(R.string.pen_league_aurora, listOf(0xFF2AF598, 0xFF08AEEA, 0xFF6A5AE0), 0, isLeagueReward = true),
    LEAGUE_EMBER(R.string.pen_league_ember, listOf(0xFFFF5F6D, 0xFFFFC371), 0, isLeagueReward = true),
    LEAGUE_FROST(R.string.pen_league_frost, listOf(0xFFB8F2FF, 0xFF4FACFE, 0xFF1B3F8B), 0, isLeagueReward = true),
    LEAGUE_MIDNIGHT(R.string.pen_league_midnight, listOf(0xFF232526, 0xFF6D5BBA, 0xFF8D58BF), 0, isLeagueReward = true),
    LEAGUE_CITRUS(R.string.pen_league_citrus, listOf(0xFFF9D423, 0xFFFF4E50), 0, isLeagueReward = true),
    LEAGUE_ROSE(R.string.pen_league_rose, listOf(0xFFFFDEE9, 0xFFE96D9E, 0xFF8E2DE2), 0, isLeagueReward = true),

    // --- Store pens (bought with gold, see StoreScreen). unlockLevel 0 like league prizes. ---
    COPPER(R.string.pen_copper, listOf(0xFFB87333, 0xFFE9A26B), 0, storePrice = 1500),
    EMERALD(R.string.pen_emerald, listOf(0xFF0B8F5A, 0xFF4CE0A0), 0, storePrice = 2500),
    RUBY(R.string.pen_ruby, listOf(0xFFB0123A, 0xFFFF5C7A), 0, storePrice = 2500),
    SAPPHIRE(R.string.pen_sapphire, listOf(0xFF1F3FBF, 0xFF5CA8FF), 0, storePrice = 3500),
    CANDY(R.string.pen_candy, listOf(0xFFFF5FA2, 0xFFFFFFFF, 0xFFFF5FA2), 0, storePrice = 5000),
    ICE(R.string.pen_ice, listOf(0xFFCFF3FF, 0xFF6FD3FF), 0, storePrice = 6500),
    SAKURA(R.string.pen_sakura, listOf(0xFFFFC1D6, 0xFFFF8FB8, 0xFFE0609A), 0, storePrice = 8000),
    SUNRISE(R.string.pen_sunrise, listOf(0xFFFFD36E, 0xFFFF7A59, 0xFFC13584), 0, storePrice = 10000, isRuneReward = true),
    NIGHT(R.string.pen_night, listOf(0xFF0F0C29, 0xFF302B63, 0xFF5B4FBF), 0, storePrice = 12500, isRuneReward = true, retired = true),
    DIAMOND(R.string.pen_diamond, listOf(0xFFE0F7FF, 0xFFB388FF, 0xFF7CE7FF), 0, storePrice = 15000, isRuneReward = true),

    // --- Premium store pens: the dearest things on the shelf, each with a stroke effect (see PenEffect). ---
    AURUM(R.string.pen_aurum, listOf(0xFFB8860B, 0xFFFFD700, 0xFFFFF1A8, 0xFFFFD700, 0xFFB8860B), 0, storePrice = 20000, effect = PenEffect.GLINT),
    PRISM(R.string.pen_prism, listOf(0xFFFF4B4B, 0xFFFFA63C, 0xFFFFE23C, 0xFF3CD16A, 0xFF3CA8FF, 0xFF8E4BFF, 0xFFFF4BB0), 0, storePrice = 30000, effect = PenEffect.FLOW),
    EMBER(R.string.pen_ember, listOf(0xFFC62828, 0xFFFF5722, 0xFFFFB300, 0xFFFFE082), 0, storePrice = 45000, effect = PenEffect.EMBERS),
    STORM(R.string.pen_storm, listOf(0xFF1E5BD8, 0xFF4FC3F7, 0xFFFFFFFF, 0xFF4FC3F7), 0, storePrice = 60000, effect = PenEffect.BOLTS),
    COSMOS(R.string.pen_cosmos, listOf(0xFF1A1055, 0xFF5B3FA8, 0xFFB388FF, 0xFF4FC3F7), 0, storePrice = 85000, effect = PenEffect.STARS),
    SPECTER(R.string.pen_specter, listOf(0xFF1B0B3A, 0xFF5E2CA5, 0xFFB44CFF, 0xFF5E2CA5), 0, storePrice = 120000, effect = PenEffect.SMOKE),
    LION(R.string.pen_lion, listOf(0xFFFFC857, 0xFFFF9A2E, 0xFFE0492B, 0xFFFFC857), 0, storePrice = 180000, effect = PenEffect.ROYAL),

    // --- Rune-map reward (st_5): found only on the rune map, never sold. Replaces the plain Night pen as that rune's gift. ---
    AURORA(R.string.pen_aurora, listOf(0xFF1FD6A0, 0xFF22B8E0, 0xFF5B6CF0, 0xFFA66BFF), 0, storePrice = 12500, isRuneReward = true, effect = PenEffect.AURORA);

    /** True when the stroke should be painted as a gradient rather than a flat colour. */
    val isGradient: Boolean get() = colors.size > 1

    val isStoreItem: Boolean get() = storePrice > 0

    /** On sale in the store (a rune reward has a price only for the rune's pay-back, see SettingsRepository.buyRune). */
    val isSoldInStore: Boolean get() = isStoreItem && !isRuneReward && !retired

    companion object {
        val DEFAULT = CLASSIC

        /** Every pen on the level ladder, in unlock order — league prizes excluded. */
        val ladder: List<PenSkin> get() = entries.filter { !it.isLeagueReward && !it.isStoreItem }

        /** The level ladder only — league prizes are earned, not reached. */
        fun unlockedFor(level: Int): List<PenSkin> =
            ladder.filter { level >= it.unlockLevel }

        /**
         * The stored pick, but only if this [level] has actually earned it —
         * same defensive reasoning as [AvatarFrame.resolve]: the selection
         * lives in plain SharedPreferences and arrives from an opponent's
         * device over Firestore, so neither source can be trusted to be legal.
         */
        fun resolve(selectedName: String?, level: Int): PenSkin {
            val selected = selectedName?.let { name -> entries.find { it.name == name } }
            return if (selected != null && level >= selected.unlockLevel) selected else DEFAULT
        }
    }
}
