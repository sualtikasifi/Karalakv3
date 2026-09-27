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
    // faceDiameterFraction/faceOffsetXFraction/faceOffsetYFraction below were
    // re-measured directly off each PNG's own transparent hole (a flood-fill
    // from the artwork's centre, see the analysis script used to produce
    // these) rather than eyeballed — several were off by close to a whole
    // pixel-visible dp at the 88dp profile badge size (PAINTER had a
    // -0.022 X offset though its hole sits dead centre; CELESTIAL_MOON's Y
    // offset was a third of what its hole actually needs), which is what
    // read as "not quite centred" on some frames and not others.
    SCRIBBLER(R.drawable.level_frame_scribbler, 0.764f, 0.001f, 0.001f, 1),
    ARTIST(R.drawable.level_frame_artist, 0.535f, 0f, 0f, 10),
    APPRENTICE(R.drawable.level_frame_apprentice, 0.44f, 0f, 0.003f, 20),
    POP_ART(R.drawable.level_frame_pop_art, 0.475f, -0.002f, -0.001f, 30),
    WOOD_PALETTE(R.drawable.level_frame_wood_palette, 0.475f, 0f, -0.007f, 40),
    MASTER_PAINTER(R.drawable.level_frame_master_painter, 0.449f, 0.002f, -0.0195f, 50),
    WATERCOLOR_BRUSHES(R.drawable.level_frame_watercolor_brushes, 0.531f, -0.002f, -0.008f, 60),
    PAINTER(R.drawable.level_frame_painter, 0.59f, 0f, 0f, 70),
    CHALK(R.drawable.level_frame_chalk, 0.584f, 0.003f, 0.004f, 80),
    GRAFFITI(R.drawable.level_frame_graffiti, 0.535f, 0f, -0.003f, 90),
    GRAND_MASTER(R.drawable.level_frame_grand_master, 0.576f, 0.003f, 0f, 100),

    // --- Store frames (bought with gold, see StoreScreen). unlockLevel 0 like league prizes. ---
    WATER_SPLASH(R.drawable.level_frame_water_splash, 0.516f, 0.001f, 0.014f, 0, storePrice = 1200),
    JASMINE_WREATH(R.drawable.level_frame_jasmine_wreath, 0.602f, 0f, -0.013f, 0, storePrice = 1500),
    CHERRY_BLOSSOM(R.drawable.level_frame_cherry_blossom, 0.6f, 0.003f, -0.004f, 0, storePrice = 1800),
    CHERRY_BLOSSOM_GOLD(R.drawable.level_frame_cherry_blossom_gold, 0.613f, 0.004f, -0.0215f, 0, storePrice = 2200),
    FLAME_RING(R.drawable.level_frame_flame_ring, 0.572f, -0.001f, -0.009f, 0, storePrice = 3000),
    ICE_CRYSTAL(R.drawable.level_frame_ice_crystal, 0.549f, 0.001f, -0.013f, 0, storePrice = 3200),
    RAINBOW_DREAM(R.drawable.level_frame_rainbow_dream, 0.607f, 0.008f, -0.005f, 0, storePrice = 4000),
    AMETHYST_CRYSTAL(R.drawable.level_frame_amethyst_crystal, 0.543f, -0.001f, -0.0215f, 0, storePrice = 4500),
    OCEAN_TREASURE(R.drawable.level_frame_ocean_treasure, 0.58f, -0.009f, -0.013f, 0, storePrice = 6000),
    TIME_EXPLORER(R.drawable.level_frame_time_explorer, 0.584f, -0.001f, -0.0225f, 0, storePrice = 6500),
    GALAXY_RING(R.drawable.level_frame_galaxy_ring, 0.582f, 0f, -0.013f, 0, storePrice = 7500),
    CELESTIAL_MOON(R.drawable.level_frame_celestial_moon, 0.506f, 0.008f, -0.06f, 0, storePrice = 8000),
    PEARL_BUTTERFLY(R.drawable.level_frame_pearl_butterfly, 0.588f, 0.004f, -0.005f, 0, storePrice = 10000),
    DRAGON_FLAME(R.drawable.level_frame_dragon_flame, 0.545f, 0.001f, -0.001f, 0, storePrice = 12000),
    SHADOW_CROWN(R.drawable.level_frame_shadow_crown, 0.545f, -0.001f, -0.017f, 0, storePrice = 13000),
    ROYAL_CROWN(R.drawable.level_frame_royal_crown, 0.537f, 0.001f, -0.0205f, 0, storePrice = 15000);

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
