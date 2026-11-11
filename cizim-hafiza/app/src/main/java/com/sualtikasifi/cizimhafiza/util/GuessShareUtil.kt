package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import android.content.Intent
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke

/**
 * Sends a "Bil Bakalım" link: the drawing's card (its word masked) with the link as the caption, or the link alone
 * when the card cannot be made. The https address is the one part every messenger turns into a tap: it opens the app
 * where it is installed (Android App Link / karalak://guess) and the page in hosting/bil everywhere else.
 */
object GuessShareUtil {
    const val BASE = "https://karalak.com/b/"

    fun link(id: String): String = BASE + id

    fun share(context: Context, linkId: String, word: String, strokes: List<DrawingStroke>) {
        val text = context.getString(R.string.guess_share_text, link(linkId))
        val sentWithPicture = strokes.any { it.isNotEmpty() } && runCatching {
            DrawingShareUtil.shareDrawingWithLink(context, word, strokes, text)
        }.isSuccess
        if (!sentWithPicture) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, null))
        }
    }
}
