package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Limits how far the system "font size" setting can grow text inside
 * fixed-size game cards (chips, tiles, chest slots). Text still grows with
 * the setting, just not past [max] — beyond that the cards were clipping
 * their labels. Free-flowing screens (lists, settings) are left alone.
 */
@Composable
fun CappedFontScale(max: Float = 1.15f, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val capped = Density(density = density.density, fontScale = density.fontScale.coerceAtMost(max))
    CompositionLocalProvider(LocalDensity provides capped, content = content)
}
