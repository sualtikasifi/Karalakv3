package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont

/**
 * The game's own popup window: a centred cream panel with a title and a
 * close button over a dimmed screen — used for pickers instead of a bottom
 * sheet sliding up from the edge.
 */
@Composable
fun AppWindowDialog(
    title: String,
    onDismiss: () -> Unit,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    /** Centre the title and drop the corner close button (for windows that end in their own confirm button). */
    centerTitle: Boolean = false,
    /** Wrap the body in a scroller when a footer is given; off for content that is sized to fit. */
    scrollBody: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            // The same parchment-in-a-wooden-frame panel as PaintedDialog, so every window in the game matches.
            NinePatch(
                res = R.drawable.league_card,
                slicePx = 100,
                edge = 24.dp,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .springIn(fromY = 44, stepMs = 0)
                    // Swallow taps on the panel itself so only the dim area closes it.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (centerTitle) Arrangement.Center else Arrangement.Start) {
                    FitText(
                        text = title,
                        style = PaintedStyle(
                            color = Color(0xFF3A2416),
                            fontSize = 23.sp,
                            textAlign = if (centerTitle) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start
                        ),
                        maxLines = 2,
                        contentAlignment = if (centerTitle) Alignment.Center else Alignment.CenterStart,
                        modifier = if (centerTitle) Modifier.fillMaxWidth() else Modifier.weight(1f)
                    )
                    if (!centerTitle) Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3A2416).copy(alpha = 0.1f))
                            .clickable(onClick = onDismiss)
                            .a11yButton(stringResource(R.string.close)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = Color(0xFF3A2416), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (footer != null) {
                    // Body scrolls, footer (the primary action) stays pinned so it can never be pushed off a short screen.
                    Column(modifier = if (scrollBody) Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()) else Modifier) { content() }
                    Spacer(modifier = Modifier.height(12.dp))
                    footer()
                } else {
                    content()
                }
            }
            }
        }
    }
}
