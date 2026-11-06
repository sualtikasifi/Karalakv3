package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.graphics.RectF

/**
 * The end card with the phone (real_end_card_phone, 1080 × 2400): where the phone's screen is cut out of it, in the
 * card's pixels. The how-to video plays behind that hole, a little past its edges, which the card's bezel covers.
 * Shared by every format that closes on this card.
 */
object EndCardPhone {
    const val LEFT = 64f
    const val TOP = 710f
    const val RIGHT = 625f
    const val BOTTOM = 1912f

    /** The video's rectangle: the hole, three pixels bigger on every side. */
    fun screen() = RectF(LEFT - 3f, TOP - 3f, RIGHT + 3f, BOTTOM + 3f)
}
