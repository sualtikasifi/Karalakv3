package com.sualtikasifi.cizimhafiza.presentation.reports.console

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource

/**
 * The dark band the developer panel and its passcode screen sit under: the lobby's painted workshop, drained of most
 * of its color and nearly lost under a warm charcoal gradient, with a faint glow of the brand's orange in one corner.
 * Enough of the picture to belong to the game; not enough to compete with a single number on the sheet.
 */
@Composable
fun ConsoleBackdrop(modifier: Modifier = Modifier, artAlpha: Float = 0.30f) {
    val drained = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.35f) }) }
    Box(modifier.background(Console.HeaderTop)) {
        Image(
            painter = cachedPainterResource(R.drawable.bg_lobby),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            colorFilter = drained,
            alpha = artAlpha,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0f to Console.HeaderTop.copy(alpha = 0.55f),
                            0.6f to Console.HeaderBottom.copy(alpha = 0.82f),
                            1f to Console.HeaderBottom.copy(alpha = 0.96f)
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(Console.Accent.copy(alpha = 0.30f), Color.Transparent),
                            center = Offset(size.width * 0.92f, size.height * 0.05f),
                            radius = size.maxDimension * 0.55f
                        )
                    )
                }
        )
    }
}

/** A round icon button for the dark band: a faint glass disc, light glyph. */
@Composable
fun HeaderIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 38.dp, tint: Color = Console.HeaderInk) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, 0.88f)
            .size(size)
            .clip(CircleShape)
            .background(Console.HeaderFill)
            .border(1.dp, Console.HeaderLine, CircleShape)
            .clickable(interaction, ripple(color = Color.White), onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

/**
 * The top of every panel screen, on the dark band: a back button, where you are (a small line over the screen's
 * name, which slides in fresh whenever it changes), and the screen's own actions on the right.
 */
@Composable
fun ConsoleTopBar(
    title: String,
    overline: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backDescription: String = "Geri",
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(60.dp)
            .padding(start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderIconButton(Icons.AutoMirrored.Filled.ArrowBack, backDescription, onBack)
        Spacer(Modifier.width(12.dp))
        AnimatedContent(
            targetState = overline to title,
            transitionSpec = {
                (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(tween(260)) { it / 3 })
                    .togetherWith(fadeOut(tween(120)) + slideOutVertically(tween(160)) { -it / 4 })
            },
            label = "topBarTitle",
            modifier = Modifier.weight(1f)
        ) { (over, name) ->
            Column {
                Text(over.uppercase(java.util.Locale("tr")), style = Console.Overline.copy(color = Console.HeaderMuted, fontSize = 10.sp, letterSpacing = 1.1.sp), maxLines = 1)
                Text(name, style = Console.Title.copy(color = Console.HeaderInk, fontSize = 18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * Whether this phone is signed in as the reviewer, always in view: the rules gate every write on a uid, a uid is
 * invisible, and a reinstall can sign the device in as someone else. Green "Yetkili" or red "Yetkisiz"; a tap tells
 * the rest.
 */
@Composable
fun IdentityChip(ok: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val dot = if (ok) Color(0xFF3DDC84) else Color(0xFFFF6B6B)
    Row(
        modifier = modifier
            .pressScale(interaction, 0.92f)
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(if (ok) Console.HeaderFill else Color(0x33FF6B6B))
            .border(1.dp, if (ok) Console.HeaderLine else Color(0x66FF6B6B), RoundedCornerShape(50))
            .clickable(interaction, ripple(color = Color.White), onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(6.dp))
        Text(
            if (ok) "Yetkili" else "Yetkisiz",
            style = Console.Label.copy(color = Console.HeaderInk, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
            maxLines = 1
        )
    }
}
