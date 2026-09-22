package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.annotation.StringRes
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame

/** Display name of a frame. Exhaustive on purpose: a new frame will not compile until it has a name (add the string in values and values-tr). */
@StringRes
fun AvatarFrame.nameRes(): Int = when (this) {
    AvatarFrame.SCRIBBLER -> R.string.frame_name_scribbler
    AvatarFrame.ARTIST -> R.string.frame_name_artist
    AvatarFrame.APPRENTICE -> R.string.frame_name_apprentice
    AvatarFrame.POP_ART -> R.string.frame_name_pop_art
    AvatarFrame.WOOD_PALETTE -> R.string.frame_name_wood_palette
    AvatarFrame.MASTER_PAINTER -> R.string.frame_name_master_painter
    AvatarFrame.WATERCOLOR_BRUSHES -> R.string.frame_name_watercolor_brushes
    AvatarFrame.PAINTER -> R.string.frame_name_painter
    AvatarFrame.CHALK -> R.string.frame_name_chalk
    AvatarFrame.GRAFFITI -> R.string.frame_name_graffiti
    AvatarFrame.GRAND_MASTER -> R.string.frame_name_grand_master
    AvatarFrame.WATER_SPLASH -> R.string.frame_name_water_splash
    AvatarFrame.JASMINE_WREATH -> R.string.frame_name_jasmine_wreath
    AvatarFrame.CHERRY_BLOSSOM -> R.string.frame_name_cherry_blossom
    AvatarFrame.CHERRY_BLOSSOM_GOLD -> R.string.frame_name_cherry_blossom_gold
    AvatarFrame.FLAME_RING -> R.string.frame_name_flame_ring
    AvatarFrame.ICE_CRYSTAL -> R.string.frame_name_ice_crystal
    AvatarFrame.RAINBOW_DREAM -> R.string.frame_name_rainbow_dream
    AvatarFrame.AMETHYST_CRYSTAL -> R.string.frame_name_amethyst_crystal
    AvatarFrame.OCEAN_TREASURE -> R.string.frame_name_ocean_treasure
    AvatarFrame.TIME_EXPLORER -> R.string.frame_name_time_explorer
    AvatarFrame.GALAXY_RING -> R.string.frame_name_galaxy_ring
    AvatarFrame.CELESTIAL_MOON -> R.string.frame_name_celestial_moon
    AvatarFrame.PEARL_BUTTERFLY -> R.string.frame_name_pearl_butterfly
    AvatarFrame.DRAGON_FLAME -> R.string.frame_name_dragon_flame
    AvatarFrame.SHADOW_CROWN -> R.string.frame_name_shadow_crown
    AvatarFrame.ROYAL_CROWN -> R.string.frame_name_royal_crown
}
