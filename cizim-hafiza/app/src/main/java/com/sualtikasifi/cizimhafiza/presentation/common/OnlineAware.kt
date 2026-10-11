package com.sualtikasifi.cizimhafiza.presentation.common

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.sualtikasifi.cizimhafiza.R

/** Whether the phone has a working internet connection (provided once at the app's root, see MainActivity). */
val LocalOnline = staticCompositionLocalOf { true }

/**
 * [action] when online; otherwise a short "internet needed" notice instead. For buttons that open a server-backed
 * feature (Quick Match, rooms, friends, league, daily challenge, account) — they stay visible but veiled.
 */
@Composable
fun onlineOnly(action: () -> Unit): () -> Unit {
    val online = LocalOnline.current
    val context = LocalContext.current
    return if (online) action else ({
        Toast.makeText(context, R.string.offline_needed, Toast.LENGTH_SHORT).show()
    })
}

/** Same for a rewarded-ad button: with no internet there is no ad, and the player is told so. */
@Composable
fun adOnly(action: () -> Unit): () -> Unit {
    val online = LocalOnline.current
    val context = LocalContext.current
    return if (online) action else ({
        Toast.makeText(context, R.string.ad_unavailable, Toast.LENGTH_SHORT).show()
    })
}

/**
 * Wraps a screen that only works online: with no internet a panel covers it saying so, with a way back. It lifts by itself
 * the moment the connection returns, so the player carries on where they were.
 */
@Composable
fun OfflineGate(onBack: () -> Unit, content: @Composable () -> Unit) {
    val online = LocalOnline.current
    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
        content()
        if (!online) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color(0xE62A1708))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                NinePatch(
                    res = R.drawable.qm_pad,
                    slicePx = 120,
                    edge = 26.dp,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 34.dp, bottom = 26.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(
                            text = androidx.compose.ui.res.stringResource(R.string.offline_screen_title),
                            style = PaintedStyle(color = androidx.compose.ui.graphics.Color(0xFF2B1A10), fontSize = 24.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        )
                        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = androidx.compose.ui.res.stringResource(R.string.offline_screen_body),
                            style = DescriptionStyle(15.sp, 20.sp)
                        )
                        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(16.dp))
                        PaintedPillButton(
                            text = androidx.compose.ui.res.stringResource(R.string.cd_back),
                            onClick = onBack,
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(0.7f)
                        )
                    }
                }
            }
        }
    }
}
