package com.sualtikasifi.cizimhafiza.domain.model

import com.sualtikasifi.cizimhafiza.R

/**
 * An unlockable, player-*chosen* ring illustration for [LevelAvatar]/
 * [presentation.common.LevelAvatar] — deliberately a separate ladder from
 * [LevelTier]. [LevelTier] still drives the fixed rank NAME text
 * ("Karalamacı"/"Çırak"/...) shown on StatisticsScreen; this enum drives only
 * the ring *graphic*, and unlike the tier (which always shows whatever band
 * the player's current level falls in) a frame stays available once earned —
 * a level-70 player can still wear the level-1 frame if they like it better,
 * see [resolve].
 *
 * [name] is persisted as the selected frame id (see
 * SettingsRepository.selectedAvatarFrameId), so — same rule as [Achievement]
 * — never rename an existing constant; add new ones instead.
 */
enum class AvatarFrame(
    val drawableRes: Int,
    /**
     * Diameter of the largest circle that fits inside this artwork's own
     * transparent hole, as a fraction of the full badge — measured off the
     * source PNG rather than eyeballed, so the level face fills the hole
     * without overlapping the ring.
     */
    val faceDiameterFraction: Float,
    /**
     * Where that hole's centre sits relative to the artwork's centre, as a
     * fraction of the badge size. Most frames are drawn dead centre (0f), but
     * a few hang extra ornament off one side — an oil ring's palette knife, a
     * palette's paint blobs — which pushes the hole off-centre; without these
     * the level number would sit visibly high or to one side inside them.
     */
    val faceOffsetXFraction: Float,
    val faceOffsetYFraction: Float,
    val unlockLevel: Int,
    /**
     * A weekly-league prize rather than a rung on the level ladder — same
     * contract as [PenSkin.isLeagueReward], including the deliberate
     * decision not to check ownership in [resolve].
     *
     * League frames need their own artwork (a transparent ring with a hole
     * for the level face, like every constant above), so they are added here
     * as the art lands. The plumbing below already keeps them out of the
     * level ladder, so adding one is a drawable and a line.
     */
    val isLeagueReward: Boolean = false,
    /** Gold price in the store; 0 means not sold there. Give a store frame unlockLevel 0 (see PenSkin.storePrice). */
    val storePrice: Int = 0
) {
    SCRIBBLER(R.drawable.level_frame_scribbler, 0.72f, 0f, 0f, 1),
    ARTIST(R.drawable.level_frame_artist, 0.51f, 0f, 0f, 10),
    APPRENTICE(R.drawable.level_frame_apprentice, 0.42f, 0f, 0f, 20),
    POP_ART(R.drawable.level_frame_pop_art, 0.45f, 0f, 0f, 30),
    WOOD_PALETTE(R.drawable.level_frame_wood_palette, 0.45f, 0f, -0.014f, 40),
    MASTER_PAINTER(R.drawable.level_frame_master_painter, 0.42f, 0f, -0.031f, 50),
    WATERCOLOR_BRUSHES(R.drawable.level_frame_watercolor_brushes, 0.49f, 0f, -0.022f, 60),
    PAINTER(R.drawable.level_frame_painter, 0.53f, -0.022f, 0f, 70),
    CHALK(R.drawable.level_frame_chalk, 0.53f, 0f, 0f, 80),
    GRAFFITI(R.drawable.level_frame_graffiti, 0.49f, 0f, 0f, 90),
    GRAND_MASTER(R.drawable.level_frame_grand_master, 0.51f, 0f, 0f, 100),

    // --- Store frames (bought with gold, see StoreScreen). unlockLevel 0 like league prizes. ---
    WATER_SPLASH(R.drawable.level_frame_water_splash, 0.50f, 0f, 0f, 0, storePrice = 1200),
    JASMINE_WREATH(R.drawable.level_frame_jasmine_wreath, 0.46f, 0f, 0f, 0, storePrice = 1500),
    CHERRY_BLOSSOM(R.drawable.level_frame_cherry_blossom, 0.44f, 0f, 0f, 0, storePrice = 1800),
    CHERRY_BLOSSOM_GOLD(R.drawable.level_frame_cherry_blossom_gold, 0.44f, 0f, 0f, 0, storePrice = 2200),
    FLAME_RING(R.drawable.level_frame_flame_ring, 0.48f, 0f, 0f, 0, storePrice = 3000),
    ICE_CRYSTAL(R.drawable.level_frame_ice_crystal, 0.42f, 0f, 0f, 0, storePrice = 3200),
    RAINBOW_DREAM(R.drawable.level_frame_rainbow_dream, 0.40f, 0f, 0f, 0, storePrice = 4000),
    AMETHYST_CRYSTAL(R.drawable.level_frame_amethyst_crystal, 0.46f, 0f, 0f, 0, storePrice = 4500),
    OCEAN_TREASURE(R.drawable.level_frame_ocean_treasure, 0.48f, 0f, 0f, 0, storePrice = 6000),
    TIME_EXPLORER(R.drawable.level_frame_time_explorer, 0.44f, 0f, 0f, 0, storePrice = 6500),
    GALAXY_RING(R.drawable.level_frame_galaxy_ring, 0.55f, 0f, 0f, 0, storePrice = 7500),
    CELESTIAL_MOON(R.drawable.level_frame_celestial_moon, 0.40f, 0.05f, -0.02f, 0, storePrice = 8000),
    PEARL_BUTTERFLY(R.drawable.level_frame_pearl_butterfly, 0.48f, 0f, 0f, 0, storePrice = 10000),
    DRAGON_FLAME(R.drawable.level_frame_dragon_flame, 0.44f, 0f, 0f, 0, storePrice = 12000),
    SHADOW_CROWN(R.drawable.level_frame_shadow_crown, 0.46f, 0f, 0f, 0, storePrice = 13000),
    ROYAL_CROWN(R.drawable.level_frame_royal_crown, 0.50f, 0f, 0f, 0, storePrice = 15000);

    // Monthly league prize frames (LEAGUE_CHAMPION_<year>_<month>) were
    // removed — the first batch of artwork did not clean up against the
    // transparency checkerboard it was screenshotted over. The plumbing for
    // one (LeagueReward, isLeagueReward, functions/src/index.ts's
    // rewardIdFor) is unaffected: a month with no frame declared here
    // resolves to nothing rather than to a broken picture (see
    // LeagueReward.find), so the league runs on pen prizes alone until a
    // clean set of frames is added back the same way.

    val isStoreItem: Boolean get() = storePrice > 0

    companion object {
        /** What every new install starts with, and what [resolve] falls back to. */
        val DEFAULT = SCRIBBLER

        /** Every frame this device has earned the right to wear at [level]. */
        /** Every frame on the level ladder, in unlock order — league prizes excluded. */
        val ladder: List<AvatarFrame> get() = entries.filter { !it.isLeagueReward && !it.isStoreItem }

        /** The level ladder only — league prizes are earned, not reached. */
        fun unlockedFor(level: Int): List<AvatarFrame> = ladder.filter { level >= it.unlockLevel }

        /** The most recently unlocked frame at [level] — used for players whose own pick we don't know (see [presentation.common.LevelAvatar]'s other-player call sites). */
        fun highestUnlockedFor(level: Int): AvatarFrame = ladder.last { level >= it.unlockLevel }

        /**
         * The frame to actually render for *this* device's own player:
         * [selectedName] (SettingsRepository.selectedAvatarFrameId) if it
         * names a real frame this [level] has unlocked, otherwise
         * [DEFAULT] — never a locked frame, and never silently upgrading to
         * "whatever's newest" the way [LevelTier] does, since the whole
         * point is that the player picks.
         */
        fun resolve(selectedName: String?, level: Int): AvatarFrame {
            val selected = selectedName?.let { name -> entries.find { it.name == name } }
            return if (selected != null && level >= selected.unlockLevel) selected else DEFAULT
        }
    }
}
