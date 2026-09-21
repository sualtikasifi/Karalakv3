package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme

/**
 * [RaisedCard] in the home screen's warm cream + sand-edge look — the drop-in
 * for the older screens (settings, league, achievements) so the whole game
 * reads as one style. Same parameters as RaisedCard; only the defaults differ.
 */
@Composable
fun WarmCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    face: Color = Color(0xFFFFF6E3),
    edge: Color = Color(0xFFD9B57A),
    raise: Dp = AppTheme.tokens.raise,
    border: Color? = Color(0xFFEBCB93),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) = RaisedCard(
    modifier = modifier,
    corner = corner,
    face = face,
    edge = edge,
    raise = raise,
    border = border,
    contentColor = contentColor,
    onClick = onClick,
    content = content
)
