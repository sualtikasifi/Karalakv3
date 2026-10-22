package com.sualtikasifi.cizimhafiza.presentation.common

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.BuildConfig
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.Moderation
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.rememberSink
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sinkWith
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sunkenArt
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont
import com.sualtikasifi.cizimhafiza.util.UpdateGate

// update_card is 1100 x 1424; the rectangles are x0, y0, x1, y1 in the picture's own pixels.
private const val ArtW = 1100f
private const val ArtH = 1424f
// The parchment runs x 100-1000, y 450-1070 below the dog, the arrow and the gift; the button is the orange pill below it.
private val TitleRect = floatArrayOf(150f, 490f, 950f, 650f)
private val MessageRect = floatArrayOf(160f, 670f, 940f, 1010f)
private val ButtonRect = floatArrayOf(170f, 1135f, 925f, 1295f)

private val UpdateInk = Color(0xFF3A2416)
private val UpdateSoftInk = Color(0xFF6B4122)
private val UpdateButtonOutline = Color(0xFF8A3A00)

private const val PLAY_MARKET = "market://details?id=com.sualtikasifi.cizimhafiza"
private const val PLAY_WEB = "https://play.google.com/store/apps/details?id=com.sualtikasifi.cizimhafiza"

/**
 * The "update to keep playing" gate over the whole app, for an install older than the lowest allowed version (see
 * [UpdateGate]). Shows nothing for everyone else, and never for the developer's own account.
 */
@Composable
fun ForcedUpdateHost() {
    val min by UpdateGate.minVersionCode.collectAsState()
    val isReviewer = remember(min) {
        runCatching {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                ?.let { it.email == Moderation.REVIEWER_EMAIL && it.isEmailVerified } == true
        }.getOrDefault(false)
    }
    if (!UpdateGate.blocks(BuildConfig.VERSION_CODE, min, isReviewer)) return
    val context = LocalContext.current
    ForcedUpdateScreen(
        title = stringResource(R.string.forced_update_title),
        message = stringResource(R.string.forced_update_message),
        buttonText = stringResource(R.string.forced_update_button),
        onUpdate = {
            // The Play app if there is one, the web page otherwise.
            val opened = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_MARKET)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
            if (!opened) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_WEB)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        },
        onExit = { (context as? Activity)?.finish() }
    )
}

/** The card itself: nothing behind it can be reached, and the back key leaves the app instead of closing the card. */
@Composable
fun ForcedUpdateScreen(
    title: String,
    message: String,
    buttonText: String,
    onUpdate: () -> Unit,
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)
    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xCC1A0E05)), contentAlignment = Alignment.Center) {
            val frameW = minOf(maxWidth - 24.dp, (maxHeight - 24.dp) * (ArtW / ArtH), 440.dp)
            val u = frameW / ArtW
            val fontScale0 = LocalDensity.current.fontScale
            val fs: (Float) -> TextUnit = { px -> (px * u.value / fontScale0).sp }
            val context = LocalContext.current
            val sheet = remember { runCatching { BackdropCache.get(context.resources, R.drawable.update_card) }.getOrNull() }
            val pxK = (sheet?.width ?: ArtW.toInt()) / ArtW

            fun Modifier.at(r: FloatArray) = offset(u * r[0], u * r[1]).size(u * (r[2] - r[0]), u * (r[3] - r[1]))

            val enter = remember { Animatable(0f) }
            LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 380f)) }
            Box(
                Modifier
                    .size(frameW, frameW * (ArtH / ArtW))
                    .graphicsLayer {
                        val e = enter.value
                        scaleX = 0.82f + 0.18f * e
                        scaleY = 0.82f + 0.18f * e
                        alpha = (e * 3f).coerceIn(0f, 1f)
                    }
            ) {
                Image(
                    painter = painterResource(R.drawable.update_card),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                FitText(
                    text = title,
                    style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(84f), color = UpdateInk, textAlign = TextAlign.Center),
                    maxLines = 2, minScale = 0.5f,
                    modifier = Modifier.at(TitleRect)
                )
                FitText(
                    text = message,
                    style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = fs(54f), lineHeight = fs(68f), color = UpdateSoftInk, textAlign = TextAlign.Center),
                    maxLines = 5, minScale = 0.6f,
                    modifier = Modifier.at(MessageRect)
                )
                val (x0, y0, x1, y1) = ButtonRect
                val sink = rememberSink(0.94f)
                Box(
                    modifier = Modifier
                        .at(ButtonRect)
                        .sunkenArt(sink, sheet, x0 * pxK, y0 * pxK, (x1 - x0) * pxK, (y1 - y0) * pxK, u * ((y1 - y0) / 2f))
                        .clickable(interactionSource = sink.source, indication = null, onClick = onUpdate)
                ) {
                    Box(Modifier.fillMaxSize().sinkWith(sink, 0.5f, 0.5f), contentAlignment = Alignment.Center) {
                        LetteredText(text = buttonText, size = fs(88f), outline = UpdateButtonOutline, maxLines = 1, modifier = Modifier.fillMaxWidth().padding(horizontal = u * 40f))
                    }
                }
            }
        }
    }
}
