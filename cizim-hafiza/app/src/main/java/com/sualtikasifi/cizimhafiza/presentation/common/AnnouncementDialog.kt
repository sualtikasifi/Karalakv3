package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sualtikasifi.cizimhafiza.R

/**
 * A notice from the developer panel (config/announcement), in the game's vine-and-parchment window with its painted orange
 * button: the window is named "Duyuru", the notice's own title is the bold heading on the parchment and its text follows
 * (shrinking to show whole, scrolling only past half size). A notice with no title is the text alone.
 */
@Composable
fun AnnouncementDialog(title: String, body: String, onClose: () -> Unit) {
    PaintedFrameDialog(
        title = stringResource(R.string.announcement_window_title),
        heading = title.trim().takeIf { it.isNotEmpty() },
        body = body.trim(),
        buttonText = stringResource(R.string.close),
        onClick = onClose
    )
}
