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
}
