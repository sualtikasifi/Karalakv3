package com.sualtikasifi.cizimhafiza.presentation.league

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.util.GameConstants
import com.sualtikasifi.cizimhafiza.domain.model.LeagueReward
import com.sualtikasifi.cizimhafiza.presentation.common.BackdropCache
import com.sualtikasifi.cizimhafiza.presentation.common.DescriptionStyle
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.rememberSink
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sinkWith
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sunkenArt
import java.text.NumberFormat

// The window is one picture (league_win_frame: the cup and ribbon on top, the cream page, two empty pills for the gold
// and the XP, the orange button between the gift boxes); all positions are in the picture's own pixels.
private const val FrameW = 1229f
private const val FrameH = 1280f
private val TitleRect = floatArrayOf(150f, 300f, 1080f, 395f)
private val PrizeRect = floatArrayOf(200f, 410f, 1030f, 585f)
private val BodyRect = floatArrayOf(150f, 598f, 1080f, 715f)
private val GoldRect = floatArrayOf(345f, 748f, 585f, 842f)
private val XpRect = floatArrayOf(765f, 748f, 1035f, 842f)
private val ButtonRect = floatArrayOf(262f, 895f, 940f, 1085f)
private val Ink = Color(0xFF3A2416)

/**
 * Shown once, the first time a won monthly prize is handed over: the rank, the prize itself (the pen drawing a stroke,
 * or the frame), what is done with it, and the monthly gold and XP every top-three finisher also gets.
 */
@Composable
internal fun LeaguePrizeDialog(reward: LeagueReward?, rank: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val frameW = minOf(maxWidth - 8.dp, 520.dp, (maxHeight - 48.dp) * (FrameW / FrameH))
            val u = frameW / FrameW
            val fontScale0 = LocalDensity.current.fontScale
            val fs: (Float) -> TextUnit = { px -> (px * u.value / fontScale0).sp }
            fun Modifier.at(r: FloatArray) = offset(u * r[0], u * r[1]).size(u * (r[2] - r[0]), u * (r[3] - r[1]))
            val context = LocalContext.current
            val sheet = remember { runCatching { BackdropCache.get(context.resources, R.drawable.league_win_frame) }.getOrNull() }
            val pxK = (sheet?.width ?: FrameW.toInt()) / FrameW
            val sink = rememberSink(0.93f)
            val enter = remember { Animatable(0f) }
            LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 360f)) }
            val number = remember { NumberFormat.getIntegerInstance() }
            Box(
                Modifier
                    .size(frameW, frameW * (FrameH / FrameW))
                    .graphicsLayer {
                        val e = enter.value
                        scaleX = 0.8f + 0.2f * e
                        scaleY = 0.8f + 0.2f * e
                        alpha = (e * 3f).coerceIn(0f, 1f)
                    }
                    // A tap on the window itself is not a tap outside it.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
                Image(
                    painter = painterResource(R.drawable.league_win_frame),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                FitText(
                    text = stringResource(R.string.league_win_title, rank),
                    style = PaintedStyle(color = Ink, fontSize = fs(92f), textAlign = TextAlign.Center),
                    maxLines = 1,
                    minScale = 0.5f,
                    modifier = Modifier.at(TitleRect)
                )
                // the prize itself: a pen draws its stroke, a frame glows; the name under it
                if (reward != null) {
                    Column(Modifier.at(PrizeRect), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        when (reward) {
                            // on a dark plate, so a pale pen (Kutup Işığı, Ayaz…) still shows on the cream page
                            is LeagueReward.Pen -> Box(
                                Modifier.fillMaxWidth(0.8f).height(u * 84f).clip(RoundedCornerShape(u * 42f)).background(Color(0xFF3A2416)).padding(horizontal = u * 28f, vertical = u * 12f)
                            ) { PenStrokePreview(reward.skin, Modifier.fillMaxSize()) }
                            is LeagueReward.Frame -> RewardSwatch(reward, u * 62f)
                        }
                        FitText(
                            text = rewardLabel(reward),
                            style = PaintedStyle(color = Color(0xFFB4440F), fontSize = fs(84f), textAlign = TextAlign.Center),
                            maxLines = 1,
                            minScale = 0.5f,
                            modifier = Modifier.fillMaxWidth().height(u * 96f)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.league_win_body),
                    style = DescriptionStyle(fs(46f), fs(56f)).copy(textAlign = TextAlign.Center),
                    modifier = Modifier.at(BodyRect).padding(top = u * 6f)
                )
                // the monthly gold and XP, in the two pills the picture leaves for them (their icons are painted in)
                FitText(
                    text = "+" + number.format(GameConstants.LEAGUE_MONTHLY_GOLD),
                    style = PaintedStyle(color = Ink, fontSize = fs(60f), textAlign = TextAlign.Center),
                    maxLines = 1, minScale = 0.5f,
                    modifier = Modifier.at(GoldRect)
                )
                FitText(
                    text = stringResource(R.string.league_reward_bonus_xp, number.format(GameConstants.LEAGUE_MONTHLY_XP)),
                    style = PaintedStyle(color = Ink, fontSize = fs(60f), textAlign = TextAlign.Center),
                    maxLines = 1, minScale = 0.5f,
                    modifier = Modifier.at(XpRect)
                )
                // the button is painted into the picture, so it sinks the way the home screen's painted buttons do
                val (x0, y0, x1, y1) = ButtonRect
                Box(
                    modifier = Modifier
                        .at(ButtonRect)
                        .sunkenArt(sink, sheet, x0 * pxK, y0 * pxK, (x1 - x0) * pxK, (y1 - y0) * pxK, u * ((y1 - y0) / 2f))
                        .clickable(interactionSource = sink.source, indication = null, onClick = onDismiss)
                ) {
                    Box(Modifier.fillMaxSize().padding(horizontal = u * 40f).sinkWith(sink, 0.5f, 0.5f), contentAlignment = Alignment.Center) {
                        LetteredText(text = stringResource(R.string.close), size = fs(88f), outline = Color(0xFF8A3A00), maxLines = 1)
                    }
                }
            }
        }
    }
}
