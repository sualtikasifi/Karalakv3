package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import com.sualtikasifi.cizimhafiza.presentation.common.AppTextField
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestSlots
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.presentation.chests.ChestBackdrop
import com.sualtikasifi.cizimhafiza.presentation.chests.ChestImage
import com.sualtikasifi.cizimhafiza.presentation.chests.borderColor
import com.sualtikasifi.cizimhafiza.presentation.chests.formatCountdown
import com.sualtikasifi.cizimhafiza.presentation.chests.onBackdrop
import com.sualtikasifi.cizimhafiza.presentation.chests.ChestsViewModel
import com.sualtikasifi.cizimhafiza.presentation.chests.accent
import com.sualtikasifi.cizimhafiza.presentation.chests.durationHours
import com.sualtikasifi.cizimhafiza.presentation.chests.glow
import com.sualtikasifi.cizimhafiza.presentation.chests.remainingMillis
import com.sualtikasifi.cizimhafiza.presentation.common.LevelAvatar
import com.sualtikasifi.cizimhafiza.presentation.common.a11yButton
import com.sualtikasifi.cizimhafiza.presentation.common.nameRes
import com.sualtikasifi.cizimhafiza.presentation.common.labelRes
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont
import java.text.NumberFormat

/**
 * A reward tile with the usual mobile-game anatomy: art on top, the reward in a two-line centred
 * caption, and an edge-to-edge footer band that says what the tap does. The band is part of the
 * tile (not a button inside it) and is what tells the player a video ad is coming — "🎬 Reklam
 * izle" while ready, "⏳ 3:59:12" while cooling down.
 */
@Composable
private fun HomeAdButton(
    ready: Boolean,
    face: List<Color>,
    edge: Color,
    label: String,
    footer: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    art: @Composable (bounce: Float) -> Unit
) {
    val t = rememberInfiniteTransition(label = "homeAd")
    val bounce by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounce"
    )
    val sheen by t.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2400, easing = androidx.compose.animation.core.LinearEasing)),
        label = "sheen"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.95f else 1f, label = "adPress")
    val colors = if (ready) face else listOf(Color(0xFFB8B2A6), Color(0xFF8F897C))
    val rim = if (ready) edge else Color(0xFF5E584B)
    val shadow = androidx.compose.ui.text.TextStyle(
        shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 3f)
    )
    Column(
        modifier = modifier
            .graphicsLayer { scaleX = press; scaleY = press }
            .chunky(Brush.verticalGradient(colors), rim, corner = 20.dp, lift = 4.dp, rim = Color.White.copy(alpha = 0.45f))
            .drawWithContent {
                drawContent()
                if (ready) {
                    val x = size.width * sheen
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.32f), Color.Transparent),
                            startX = x - 22.dp.toPx(),
                            endX = x + 22.dp.toPx()
                        )
                    )
                }
            }
            .clickable(interactionSource = interaction, indication = null, enabled = ready, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            art(if (ready) bounce else 0f)
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            FitText(text = label, color = Color.White, maxSp = 13f, minSp = 8f, twoLines = true, style = shadow)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Color.Black.copy(alpha = if (ready) 0.30f else 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.padding(horizontal = 9.dp)) {
                FitText(text = footer, color = Color.White, maxSp = 9.5f, minSp = 7f)
            }
        }
    }
}

/** Left of the daily card: watch an ad for 500 gold, available again every four hours. */
@Composable
internal fun AdGoldButton(nextAtMillis: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val now = rememberNowUntil(nextAtMillis)
    val remaining = nextAtMillis - now
    val ready = remaining <= 0
    HomeAdButton(
        ready = ready,
        face = listOf(Color(0xFFFFDB5C), Color(0xFFF59E0B)),
        edge = Color(0xFFB36B00),
        label = stringResource(R.string.home_ad_gold_label),
        footer = if (ready) "🎬 " + stringResource(R.string.home_ad_watch) else "⏳ " + hms(remaining / 1000),
        onClick = onClick,
        modifier = modifier
    ) { bounce ->
        Image(
            painter = painterResource(R.drawable.icon_gold_coin),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    translationY = -6.dp.toPx() * bounce
                    rotationZ = (bounce - 0.5f) * 12f
                    alpha = if (ready) 1f else 0.6f
                }
        )
    }
}

/** Right of the daily card: one free mid-tier chest per day for an ad, refreshed at midnight. */
@Composable
internal fun AdChestButton(availableToday: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val midnight = remember(availableToday) { java.time.LocalDate.now().plusDays(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val now = rememberNowUntil(if (availableToday) 0L else midnight)
    val remaining = midnight - now
    HomeAdButton(
        ready = availableToday,
        face = listOf(Color(0xFF6CC3FF), Color(0xFF2C7FDB)),
        edge = Color(0xFF14549A),
        label = stringResource(R.string.home_ad_chest_label),
        footer = if (availableToday) "🎬 " + stringResource(R.string.home_ad_watch) else "⏳ " + hms(remaining / 1000),
        onClick = onClick,
        modifier = modifier
    ) { bounce ->
        ChestImage(
            tier = ChestTier.GOLD,
            width = 50.dp,
            modifier = Modifier.graphicsLayer {
                rotationZ = (bounce - 0.5f) * 10f
                translationY = -4.dp.toPx() * bounce
                alpha = if (availableToday) 1f else 0.6f
            }
        )
    }
}

/**
 * A comet of light travelling once around the rounded edge: a bright head with a fading tail,
 * drawn as a sweep-gradient stroke whose shader is rotated each frame (rotating the canvas
 * instead would spin the rounded rectangle itself).
 */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOrbit(angleDegrees: Float, cornerPx: Float) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val shader = android.graphics.SweepGradient(
        cx, cy,
        intArrayOf(
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.argb(120, 255, 236, 150),
            android.graphics.Color.argb(255, 255, 255, 255),
            android.graphics.Color.TRANSPARENT
        ),
        floatArrayOf(0f, 0.55f, 0.85f, 0.985f, 1f)
    ).apply {
        setLocalMatrix(android.graphics.Matrix().apply { postRotate(angleDegrees, cx, cy) })
    }
    val brush = object : androidx.compose.ui.graphics.ShaderBrush() {
        override fun createShader(size: androidx.compose.ui.geometry.Size) = shader
    }
    val stroke = 3.dp.toPx()
    val inset = stroke / 2f
    val rectSize = androidx.compose.ui.geometry.Size(w - stroke, h - stroke)
    // Soft halo under the sharp line.
    drawRoundRect(
        brush = brush,
        topLeft = Offset(inset, inset),
        size = rectSize,
        cornerRadius = CornerRadius(cornerPx),
        alpha = 0.35f,
        style = Stroke(width = stroke * 3f)
    )
    drawRoundRect(
        brush = brush,
        topLeft = Offset(inset, inset),
        size = rectSize,
        cornerRadius = CornerRadius(cornerPx),
        style = Stroke(width = stroke)
    )
}
