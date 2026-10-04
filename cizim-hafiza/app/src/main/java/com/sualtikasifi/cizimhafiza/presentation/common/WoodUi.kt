package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R

import androidx.compose.foundation.layout.ColumnScope

// bg_offline is this size; the title sign is painted on it, so the title is placed by fractions of the picture.
private const val ArtW = 841f
private const val ArtH = 1870f
val InkBrown = Color(0xFF5A3A1A)

/**
 * The workshop scene shared by the offline and create-room screens: the painted background with its hanging sign
 * carrying [title], a scrolling stack of wooden sections in the middle, and one action pinned below.
 */
@Composable
fun WoodScreen(
    title: String,
    onBack: () -> Unit,
    action: @Composable (Modifier) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val s = maxOf(widthPx / ArtW, heightPx / ArtH)
        val offX = (widthPx - ArtW * s) / 2f
        val offY = (heightPx - ArtH * s) / 2f
        fun yOf(fraction: Float): Dp = with(density) { (offY + ArtH * s * fraction).toDp() }
        fun len(artPx: Float): Dp = with(density) { (artPx * s).toDp() }

        Image(
            painter = painterResource(R.drawable.bg_offline),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        val signCentreX = with(density) { (offX + ArtW * s * 0.505f).toDp() }
        Box(
            modifier = Modifier.offset(x = signCentreX - len(300f), y = yOf(0.172f) - len(34f)).width(len(600f)),
            contentAlignment = Alignment.Center
        ) {
            val base = TextStyle(fontSize = with(density) { (46f * s).toSp() }, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(title, style = base.copy(color = Color(0xFF5A2E0C), drawStyle = Stroke(width = with(density) { 8f * s }, join = StrokeJoin.Round)), maxLines = 1)
            Text(title, style = base.copy(color = Color.White), maxLines = 1)
        }

        val startHeight = 92.dp
        // Everything between the sign and the action scrolls; the cards melt away at the top edge.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = yOf(0.222f), bottom = startHeight + 14.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fade = 28.dp.toPx().coerceAtMost(size.height)
                    drawRect(
                        brush = Brush.verticalGradient(colorStops = arrayOf(0f to Color.Transparent, (fade / size.height) to Color.Black, 1f to Color.Black)),
                        blendMode = BlendMode.DstIn
                    )
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )

        action(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 6.dp).height(startHeight))

        ScreenTopActions(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))
    }
}

/**
 * Draws [res] stretched to any size without squashing its edges: the picture is cut into nine pieces at
 * [slicePx] from each side and only the middle pieces stretch; the corners are drawn [edge] wide.
 */
@Composable
fun NinePatch(
    res: Int,
    slicePx: Int,
    edge: Dp,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.ColorFilter? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val img: ImageBitmap = ImageBitmap.imageResource(res)
    val edgePx = with(LocalDensity.current) { edge.roundToPx() }
    Box(
        modifier = modifier.drawBehind {
            val w = size.width.toInt()
            val h = size.height.toInt()
            val d = minOf(edgePx, w / 2, h / 2)
            val sx = intArrayOf(0, slicePx, img.width - slicePx, img.width)
            val sy = intArrayOf(0, slicePx, img.height - slicePx, img.height)
            val dx = intArrayOf(0, d, w - d, w)
            val dy = intArrayOf(0, d, h - d, h)
            for (r in 0..2) for (c in 0..2) {
                drawImage(
                    image = img,
                    srcOffset = IntOffset(sx[c], sy[r]),
                    srcSize = IntSize(sx[c + 1] - sx[c], sy[r + 1] - sy[r]),
                    dstOffset = IntOffset(dx[c], dy[r]),
                    dstSize = IntSize(dx[c + 1] - dx[c], dy[r + 1] - dy[r]),
                    filterQuality = FilterQuality.Medium,
                    colorFilter = tint
                )
            }
        },
        content = content
    )
}

/** A wooden panel with a paper ribbon on its top edge carrying the section's name. */
@Composable
fun WoodSection(title: String, content: @Composable () -> Unit) {
    val ribbonHeight = 40.dp
    Box(modifier = Modifier.fillMaxWidth()) {
        NinePatch(
            res = R.drawable.offline_panel,
            slicePx = 46,
            edge = 17.dp,
            modifier = Modifier.fillMaxWidth().padding(top = ribbonHeight / 2)
        ) {
            Box(modifier = Modifier.padding(start = 17.dp, end = 17.dp, top = ribbonHeight / 2 + 16.dp, bottom = 16.dp)) { content() }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.TopCenter).widthIn(min = 190.dp).height(ribbonHeight)
        ) {
            Image(
                painter = painterResource(R.drawable.offline_ribbon),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = title,
                style = TextStyle(color = InkBrown, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 30.dp)
            )
        }
    }
}


/** An option: cream when free, orange when chosen, and a little larger while chosen. */
@Composable
fun ChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    textSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    maxLines: Int = 1
) {
    val scale by animateFloatAsState(if (selected) 1.04f else 1f, label = "pill")
    NinePatch(
        res = if (selected) R.drawable.offline_pill_on else R.drawable.offline_pill_off,
        slicePx = 46,
        edge = height / 2,
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = if (selected) Color.White else InkBrown,
                fontSize = textSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = textSize * 1.1f,
                shadow = if (selected) androidx.compose.ui.graphics.Shadow(Color(0xFF8A3A00), androidx.compose.ui.geometry.Offset(0f, 2f), 3f) else null
            ),
            maxLines = maxLines,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun StartButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "start")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(509f / 123f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Image(painter = painterResource(R.drawable.offline_start), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        val base = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Text(text, style = base.copy(color = Color(0xFF0B4F5C), drawStyle = Stroke(width = 8f, join = StrokeJoin.Round)), maxLines = 1)
        Text(text, style = base.copy(color = Color.White), maxLines = 1)
    }
}

