package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One disc per daily-challenge word: green with a white tick for a right answer, red with a white cross for a wrong
 * one, a faint empty disc for a word not played yet — each with the same thin white rim. The home card and the
 * result screen both use it, so the day's result looks the same wherever it is shown.
 */
@Composable
fun DailyPips(
    flags: List<Boolean>,
    count: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    gap: Dp = size * 0.25f,
    emptyColor: Color = Color.White.copy(alpha = 0.2f),
    rimColor: Color = Color.White
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val flag = flags.getOrNull(index)
            Box(
                modifier = Modifier
                    .popIn(trigger = flag != null, delayMs = index * 110)
                    .size(size)
                    .background(
                        when (flag) {
                            true -> Color(0xFF2EA043)
                            false -> Color(0xFFE53935)
                            null -> emptyColor
                        },
                        CircleShape
                    )
                    .border((size.value * 0.06f).coerceIn(1f, 2.5f).dp, rimColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when (flag) {
                    true -> Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.fillMaxSize(0.68f))
                    false -> Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.fillMaxSize(0.68f))
                    null -> Unit
                }
            }
        }
    }
}
