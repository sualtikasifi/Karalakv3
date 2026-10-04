package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R

/**
 * The painted page used by the quieter screens (Settings, Feedback…): the wood wall, the hanging sign with the mascot
 * leaning on it carrying [title], a painted back button, and a scrolling body underneath that melts away at its top edge.
 */
@Composable
fun PaintedPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bodyScroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_result_wood),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        val signWidth = 330.dp
        val signHeight = signWidth * (303f / 821f)
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Box(modifier = Modifier.fillMaxWidth().height(signHeight + 14.dp), contentAlignment = Alignment.TopCenter) {
                Box(modifier = Modifier.padding(top = 6.dp).width(signWidth).height(signHeight)) {
                    Image(
                        painter = painterResource(R.drawable.page_sign),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier.offset(x = signWidth * 0.03f, y = signHeight * 0.26f).size(signWidth * 0.56f, signHeight * 0.5f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = PaintedStyle(
                                color = Color(0xFF2B1A10),
                                fontSize = if (title.length > 11) 22.sp else 30.sp,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val fade = 16.dp.toPx().coerceAtMost(size.height)
                        drawRect(
                            brush = Brush.verticalGradient(colorStops = arrayOf(0f to Color.Transparent, (fade / size.height) to Color.Black, 1f to Color.Black)),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    .then(if (bodyScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp)
                    .navigationBarsPadding(),
                content = content
            )
        }
        Image(
            painter = painterResource(R.drawable.join_back),
            contentDescription = stringResource(R.string.cd_back),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 14.dp, top = 12.dp)
                .size(54.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack)
        )
    }
}

/** A parchment row with a round coloured icon on the left, used by the list screens. */
@Composable
fun PaintedRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    badge: Boolean = false,
    trailing: @Composable (() -> Unit)? = null
) {
    NinePatch(
        res = R.drawable.league_card,
        slicePx = 100,
        edge = 20.dp,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(2.dp, CircleShape)
                        .background(Brush.verticalGradient(listOf(lighten(iconColor), iconColor)), CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                if (badge) {
                    Box(
                        Modifier.align(Alignment.TopEnd).size(12.dp).background(Color(0xFFE53935), CircleShape).border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = PaintedStyle(color = InkBrown, fontSize = 17.sp, textAlign = TextAlign.Start), maxLines = 1)
                if (subtitle != null) {
                    Text(subtitle, style = DescriptionStyle(12.sp, 15.sp).copy(textAlign = TextAlign.Start), maxLines = 2)
                }
            }
            if (trailing != null) trailing()
        }
    }
}

private fun lighten(c: Color): Color = androidx.compose.ui.graphics.lerp(c, Color.White, 0.28f)

/** The game's switch: an orange track with a white knob when on, a grey one when off. */
@Composable
fun PaintedSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val track = if (checked) Brush.verticalGradient(listOf(Color(0xFFFF9A3C), Color(0xFFF26A1B))) else Brush.verticalGradient(listOf(Color(0xFFC9C3BA), Color(0xFFB4ADA3)))
    val knobX = androidx.compose.animation.core.animateDpAsState(if (checked) 24.dp else 2.dp, label = "knob").value
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 30.dp)
            .background(track, RoundedCornerShape(50))
            .border(1.5.dp, if (checked) Color(0xFFC4500F) else Color(0xFF9C948A), RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobX, y = 2.dp)
                .size(24.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}
