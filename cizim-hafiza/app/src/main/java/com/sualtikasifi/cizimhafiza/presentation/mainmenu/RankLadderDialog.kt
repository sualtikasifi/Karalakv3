package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState
import com.sualtikasifi.cizimhafiza.domain.model.LevelTier
import com.sualtikasifi.cizimhafiza.domain.model.PlayerLevel
import com.sualtikasifi.cizimhafiza.presentation.common.BlurredSceneFill
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.a11yButton
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import com.sualtikasifi.cizimhafiza.presentation.common.pressable

// The page is one painted picture (rank_ladder_bg, 841 x 1870 units): the workshop, the ribbon, the close button and six empty
// rows, each with its rank's picture on the left. Every box below is a rectangle of that picture; the words are laid over it.
private const val RankArtW = 841f
private const val RankArtH = 1870f

/** The first row's top edge, the distance from one row to the next and a row's height, in picture units. */
private const val RowTop = 496f
private const val RowPitch = 172f
private const val RowH = 154f
private const val RowLeft = 91f
private const val RowRight = 749f

private val RankInk = Color(0xFF3B2314)
private val RankRibbonOutline = Color(0xFF6B0F3F)
private val RankCurrentOrange = Color(0xFFFD6405)
private val RankCurrentTint = Color(0xFFFFE7CE)

/**
 * "Rütbeler": every rank, the level it opens at, and where the player currently stands — on the painted ladder page. The current
 * rank's row is picked out in orange, the ranks already reached are marked, and the ones still ahead are dimmed with the XP left.
 */
@Composable
internal fun RankLadderDialog(progress: LevelProgressState, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val unit: Dp = minOf(maxWidth / RankArtW, maxHeight / RankArtH)
            val offX = (maxWidth - unit * RankArtW) / 2
            val offY = (maxHeight - unit * RankArtH) / 2
            val us = unit.value
            val fontScale = LocalDensity.current.fontScale
            fun fs(art: Float) = (art * us / fontScale).sp
            fun box(x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
                Modifier.offset(offX + unit * x0, offY + unit * y0).size(unit * (x1 - x0), unit * (y1 - y0))

            val scene = cachedPainterResource(R.drawable.rank_ladder_bg)
            val enter = remember { Animatable(0f) }
            LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 380f)) }

            Box(Modifier.fillMaxSize().graphicsLayer { alpha = enter.value.coerceIn(0f, 1f) }) {
                // a screen taller or wider than the picture: its edges are filled with a blurred copy of the same scene
                if (offX > 1.dp || offY > 1.dp) {
                    BlurredSceneFill(R.drawable.rank_ladder_bg, scene, 20.dp)
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
                }
                // an offscreen layer, so the current row's tint can multiply the picture underneath it
                Box(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
                    Image(scene, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = box(0f, 0f, RankArtW, RankArtH))

                    // ---- the ribbon: the title -------------------------------------------------------------------
                    LetteredText(
                        text = stringResource(R.string.rank_ladder_title),
                        size = fs(68f),
                        outline = RankRibbonOutline,
                        modifier = box(225f, 288f, 615f, 376f),
                        minScale = 0.5f,
                        title = true
                    )

                    // ---- the line under it: the rank the player holds now ---------------------------------------
                    FitText(
                        text = stringResource(R.string.rank_ladder_subtitle, stringResource(progress.tier.rank.nameRes)),
                        style = PaintedStyle(color = RankInk, fontSize = fs(31f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        modifier = box(182f, 408f, 660f, 490f),
                        maxLines = 2
                    )

                    // ---- the six rows ---------------------------------------------------------------------------
                    LevelTier.entries.forEachIndexed { i, tier ->
                        val reached = progress.level >= tier.minLevel
                        val isCurrent = tier == progress.tier
                        val xpAway = (PlayerLevel.totalXpForLevel(tier.minLevel) - progress.totalXp).coerceAtLeast(0)
                        val top = RowTop + RowPitch * i
                        val shape = RoundedCornerShape(unit * 46f)

                        if (isCurrent) {
                            // the row picked out: its face tinted peach (multiplied, so the rank's picture keeps its colours) and an orange rim
                            Box(box(RowLeft, top, RowRight, top + RowH).clip(shape).drawWithContent {
                                drawRect(RankCurrentTint, blendMode = BlendMode.Multiply)
                            })
                            Box(box(RowLeft - 2f, top - 2f, RowRight + 2f, top + RowH + 2f).border(unit * 4f, RankCurrentOrange, shape))
                        } else if (!reached) {
                            // a rank still ahead: its picture is veiled, so the ladder shows what is coming without shouting
                            Box(box(RowLeft, top, RowRight, top + RowH).clip(shape).background(Color(0xFFFFF6EA).copy(alpha = 0.5f)))
                        }

                        // rank name and level, centred between the picture and the status pill
                        val dim = if (reached) 1f else 0.62f
                        val name = stringResource(tier.rank.nameRes)
                        val level = stringResource(R.string.rank_ladder_unlock_level, tier.minLevel)
                        FitText(
                            text = name,
                            style = PaintedStyle(color = RankInk, fontSize = fs(46f), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                            modifier = box(318f, top + 22f, 540f, top + 84f).alpha(dim),
                            minScale = 0.5f
                        )
                        FitText(
                            text = level,
                            style = PaintedStyle(color = RankInk.copy(alpha = 0.72f), fontSize = fs(30f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                            modifier = box(318f, top + 84f, 540f, top + 130f).alpha(dim),
                            minScale = 0.5f
                        )

                        // the status pill: the current rank (orange), reached (green) or the XP still to earn (plain)
                        val pillBg: Color
                        val pillInk: Color
                        val pillText: String
                        when {
                            isCurrent -> { pillBg = RankCurrentOrange; pillInk = Color.White; pillText = stringResource(R.string.rank_ladder_current) }
                            reached -> { pillBg = Color(0xFF3DAA55); pillInk = Color.White; pillText = "✓ " + stringResource(R.string.rank_ladder_reached) }
                            else -> { pillBg = Color(0xFFE9D9C6); pillInk = RankInk.copy(alpha = 0.8f); pillText = stringResource(R.string.rank_ladder_remaining, xpAway) }
                        }
                        Box(
                            box(548f, top + 47f, 737f, top + 107f).clip(RoundedCornerShape(50)).background(pillBg),
                            contentAlignment = Alignment.Center
                        ) {
                            FitText(
                                text = pillText,
                                style = PaintedStyle(color = pillInk, fontSize = fs(27f), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                                modifier = Modifier.fillMaxSize().padding(horizontal = unit * 14f),
                                minScale = 0.5f
                            )
                        }
                    }

                    // ---- the painted close button in the ribbon's corner -----------------------------------------
                    Box(box(728f, 338f, 812f, 424f).pressable(pressedScale = 0.9f, onClick = onDismiss).a11yButton(stringResource(R.string.close)))
                }
            }
        }
    }
}
