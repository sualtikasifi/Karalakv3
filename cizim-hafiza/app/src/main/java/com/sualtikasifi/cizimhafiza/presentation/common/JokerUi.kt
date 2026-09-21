package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.JokerType

@StringRes
fun JokerType.labelRes(): Int = when (this) {
    JokerType.FIRST_LETTER -> R.string.joker_first_letter
    JokerType.LETTER_COUNT -> R.string.joker_letter_count
    JokerType.EXTRA_TIME -> R.string.joker_extra_time
}

@StringRes
fun JokerType.descRes(): Int = when (this) {
    JokerType.FIRST_LETTER -> R.string.joker_first_letter_desc
    JokerType.LETTER_COUNT -> R.string.joker_letter_count_desc
    JokerType.EXTRA_TIME -> R.string.joker_extra_time_desc
}

@StringRes
fun JokerType.shortRes(): Int = when (this) {
    JokerType.FIRST_LETTER -> R.string.joker_first_letter_short
    JokerType.LETTER_COUNT -> R.string.joker_letter_count_short
    JokerType.EXTRA_TIME -> R.string.joker_extra_time_short
}

fun JokerType.icon(): ImageVector = when (this) {
    JokerType.FIRST_LETTER -> Icons.Filled.TextFields
    JokerType.LETTER_COUNT -> Icons.Filled.FormatListNumbered
    JokerType.EXTRA_TIME -> Icons.Filled.Timer
}

fun JokerType.tint(): Color = when (this) {
    JokerType.FIRST_LETTER -> Color(0xFF8E4FE0)
    JokerType.LETTER_COUNT -> Color(0xFF1E8FD6)
    JokerType.EXTRA_TIME -> Color(0xFFE8672A)
}

/** A small in-game joker button: icon, short name and how many are left. Dimmed when none are left or already used. */
@Composable
fun JokerButton(type: JokerType, count: Int, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val active = enabled && count > 0
    Row(
        modifier = modifier
            .alpha(if (active) 1f else 0.45f)
            .clip(RoundedCornerShape(50))
            .background(type.tint())
            .clickable(enabled = active, onClick = onClick)
            .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        JokerArt(type, 26.dp)
        Text(
            text = stringResource(type.shortRes()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false)
        )
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = type.tint()
            )
        }
    }
}

fun JokerType.artRes(): Int = when (this) {
    JokerType.FIRST_LETTER -> R.drawable.joker_first_letter
    JokerType.LETTER_COUNT -> R.drawable.joker_letter_count
    JokerType.EXTRA_TIME -> R.drawable.joker_extra_time
}

/**
 * The joker's own illustration. The supplied art carries a cream margin
 * around its rounded tile, so it is scaled up inside a rounded clip: the
 * tile fills [size] and the margin falls outside the clip.
 */
@Composable
fun JokerArt(type: JokerType, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.24f))) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(type.artRes()),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = 1.22f; scaleY = 1.22f }
        )
    }
}
