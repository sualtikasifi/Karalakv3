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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
val InkBrown = Color(0xFF5A321F)
/** Titles: white fill, dark-brown outline. */
val TitleOutline = Color(0xFF5A2815)
/** Descriptions and body lines laid over paper. */
val DescriptionInk = Color(0xFF5A321F)
/** Orange lettering for the white button. */
val ButtonOrange = Color(0xFFF47721)

/**
 * The lettering of every painted screen: the app's display face (Baloo 2) with the font's built-in top/bottom padding
 * removed and the line height set, so a label sits truly in the middle of whatever it is laid over.
 */
fun PaintedStyle(
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    shadow: androidx.compose.ui.graphics.Shadow? = null
): TextStyle = TextStyle(
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight ?: FontWeight.ExtraBold,
    fontFamily = com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont,
    textAlign = textAlign,
    letterSpacing = letterSpacing,
    shadow = shadow,
    lineHeight = when {
        lineHeight != androidx.compose.ui.unit.TextUnit.Unspecified -> lineHeight
        fontSize != androidx.compose.ui.unit.TextUnit.Unspecified -> fontSize * 1.2f
        else -> androidx.compose.ui.unit.TextUnit.Unspecified
    },
    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
        alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
        trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.None
    )
)

/**
 * The game's lettering: Baloo 2 ExtraBold, [fill] over a rounded [outline] (about 5 px on a 1080-px screen at title size,
 * thinner for smaller text) and a soft drop shadow. With no outline (orange on a white button) a warm shadow does the job.
 */
@Composable
fun LetteredText(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    fill: Color = Color.White,
    outline: Color? = TitleOutline,
    weight: FontWeight = FontWeight.ExtraBold,
    maxLines: Int = 1,
    /** How far the lettering may shrink to fit the space it is given before it is ellipsised instead. */
    minScale: Float = 0.55f
) {
    val density = LocalDensity.current
    // The outline is drawn outside the glyphs, so the space it takes is kept out of what the letters may fill.
    val strokeDp0 = (size.value * 0.11f).coerceIn(1.3f, 4.2f)
    val full = PaintedStyle(fontSize = size, fontWeight = weight, textAlign = TextAlign.Center)
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // One fitted size for both layers: the outline and the fill have to break and shrink identically.
        val scale = rememberFitScale(text, full, maxLines, minScale)
        val base = full.scaledBy(scale)
        val strokeDp = (strokeDp0 * scale).coerceAtLeast(1.1f)
        if (outline != null) {
            Text(
                text,
                style = base.copy(
                    color = outline,
                    drawStyle = Stroke(width = with(density) { strokeDp.dp.toPx() }, join = StrokeJoin.Round),
                    shadow = androidx.compose.ui.graphics.Shadow(Color(0x66000000), androidx.compose.ui.geometry.Offset(0f, with(density) { 2.dp.toPx() }), with(density) { 3.dp.toPx() })
                ),
                maxLines = maxLines,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Text(
            text,
            style = base.copy(
                color = fill,
                shadow = if (outline == null) androidx.compose.ui.graphics.Shadow(Color(0x66A04000), androidx.compose.ui.geometry.Offset(0f, with(density) { 1.5.dp.toPx() }), with(density) { 2.dp.toPx() }) else null
            ),
            maxLines = maxLines,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

/** Body text on paper: Baloo 2 SemiBold in the calm brown. */
fun DescriptionStyle(size: androidx.compose.ui.unit.TextUnit, lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified): TextStyle =
    PaintedStyle(color = DescriptionInk, fontSize = size, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = lineHeight)

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
        val offY = 0f
        fun yOf(fraction: Float): Dp = with(density) { (offY + ArtH * s * fraction).toDp() }
        fun len(artPx: Float): Dp = with(density) { (artPx * s).toDp() }

        Image(
            painter = painterResource(R.drawable.bg_offline),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        val signCentreX = with(density) { (offX + ArtW * s * 0.505f).toDp() }
        Box(
            modifier = Modifier.offset(x = signCentreX - len(300f), y = yOf(0.172f) - len(47f)).width(len(600f)),
            contentAlignment = Alignment.Center
        ) {
            LetteredText(title, with(density) { (46f * s).toSp() })
        }

        val compact = maxHeight < 780.dp
        val startHeight = if (compact) 80.dp else 92.dp
        // Everything between the sign and the action scrolls; the cards melt away at the top edge.
        androidx.compose.runtime.CompositionLocalProvider(LocalPanelScale provides if (compact) 0.85f else 1f) {
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
        }

        action(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 6.dp).height(startHeight))

        Image(
            painter = painterResource(R.drawable.join_back),
            contentDescription = stringResource(R.string.cd_back),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
                .size(56.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack)
        )
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
    sliceYPx: Int = slicePx,
    edgeY: Dp = edge,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val img: ImageBitmap = ImageBitmap.imageResource(res)
    val edgePx = with(LocalDensity.current) { edge.roundToPx() }
    val edgeYPx = with(LocalDensity.current) { edgeY.roundToPx() }
    Box(
        modifier = modifier.drawBehind {
            val w = size.width.toInt()
            val h = size.height.toInt()
            val d = minOf(edgePx, w / 2, h / 2)
            val dY = minOf(edgeYPx, w / 2, h / 2)
            val sx = intArrayOf(0, slicePx, img.width - slicePx, img.width)
            val sy = intArrayOf(0, sliceYPx, img.height - sliceYPx, img.height)
            val dx = intArrayOf(0, d, w - d, w)
            val dy = intArrayOf(0, dY, h - dY, h)
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
            slicePx = 92,
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
                style = PaintedStyle(
                    color = InkBrown,
                    fontSize = when {
                        title.length <= 26 -> 16.sp
                        title.length <= 32 -> 14.sp
                        else -> 12.5.sp
                    },
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 26.dp)
            )
        }
    }
}


/** 1 on a normal phone; a little less on a short screen so the whole setup still fits without scrolling. */
val LocalPanelScale = androidx.compose.runtime.compositionLocalOf { 1f }

/**
 * One wooden panel holding several labelled choice rows — the compact form of the setup screens, so everything fits
 * the screen at once instead of being a stack of separate framed sections.
 */
@Composable
fun CompactPanel(content: @Composable ColumnScope.() -> Unit) {
    val k = LocalPanelScale.current
    NinePatch(
        res = R.drawable.offline_panel,
        slicePx = 92,
        edge = 17.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp * k, bottom = 14.dp * k),
            verticalArrangement = Arrangement.spacedBy(8.dp * k),
            content = content
        )
    }
}

/** A small left-aligned caption above a row of choices. */
@Composable
fun PanelRow(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = PaintedStyle(
                color = Color(0xFFFFEBC8), fontSize = 13.5.sp, textAlign = TextAlign.Start,
                shadow = androidx.compose.ui.graphics.Shadow(Color(0xAA2A1005), androidx.compose.ui.geometry.Offset(0f, 2f), 3f)
            ),
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
        )
        content()
    }
}

/** A single-line text field painted as a cream pill. */
@Composable
fun PillField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default
) {
    NinePatch(
        res = R.drawable.offline_pill_off,
        slicePx = 90,
        edge = 20.dp,
        modifier = modifier.fillMaxWidth().height(40.dp * LocalPanelScale.current).graphicsLayer { alpha = if (enabled) 1f else 0.7f }
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = PaintedStyle(color = InkBrown, fontSize = 16.sp, textAlign = TextAlign.Start),
            keyboardOptions = keyboardOptions,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(ButtonOrange),
            modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth().padding(horizontal = 18.dp)
        )
    }
}

/** Choices laid out [columns] to a row, every cell the same width. */
@Composable
fun <T> ChoiceGrid(
    items: List<T>,
    columns: Int,
    pillHeight: Dp,
    textSize: androidx.compose.ui.unit.TextUnit,
    label: @Composable (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    maxLines: Int = 1
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { item ->
                    ChoicePill(
                        label = label(item),
                        selected = isSelected(item),
                        onClick = { onSelect(item) },
                        modifier = Modifier.weight(1f),
                        height = pillHeight * LocalPanelScale.current,
                        textSize = textSize,
                        maxLines = maxLines
                    )
                }
                repeat(columns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
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
        slicePx = 90,
        edge = height / 2,
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    ) {
        // Shrinks to stay on its line rather than wrapping ("🐶 Hayvanlar" used to break into emoji-over-word).
        FitText(
            text = label,
            style = PaintedStyle(
                color = if (selected) Color.White else InkBrown,
                fontSize = textSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = textSize * 1.1f,
                shadow = if (selected) androidx.compose.ui.graphics.Shadow(Color(0xFF8A3A00), androidx.compose.ui.geometry.Offset(0f, 2f), 3f) else null
            ),
            maxLines = maxLines,
            minScale = 0.62f,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 6.dp)
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
            .aspectRatio(1015f / 246f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Image(painter = painterResource(R.drawable.offline_start), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        LetteredText(text, 30.sp, outline = Color(0xFF0B4F5C))
    }
}

