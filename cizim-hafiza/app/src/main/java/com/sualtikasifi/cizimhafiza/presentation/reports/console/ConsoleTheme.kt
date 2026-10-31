package com.sualtikasifi.cizimhafiza.presentation.reports.console

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Material's own pieces (dialogs, switches, sliders, progress, text buttons, and every `MaterialTheme.typography`
 * a panel still reads) pointed at the console's tokens, so nothing in the developer panel falls back to the game's
 * painted display face or its palette. Wraps the whole panel, its passcode screen included.
 */
@Composable
fun ConsoleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ConsoleColors, typography = ConsoleTypography, shapes = ConsoleShapes) {
        CompositionLocalProvider(
            LocalContentColor provides Console.Ink,
            LocalTextStyle provides ConsoleTypography.bodyMedium,
            content = content
        )
    }
}

private val ConsoleColors = lightColorScheme(
    primary = Console.Accent,
    onPrimary = Color.White,
    primaryContainer = Console.AccentSoft,
    onPrimaryContainer = Console.AccentInk,
    inversePrimary = Console.AccentSoft,
    secondary = Console.Blue,
    onSecondary = Color.White,
    secondaryContainer = Console.BlueSoft,
    onSecondaryContainer = Console.Blue,
    tertiary = Console.Teal,
    onTertiary = Color.White,
    tertiaryContainer = Console.TealSoft,
    onTertiaryContainer = Console.Teal,
    background = Console.Bg,
    onBackground = Console.Ink,
    surface = Console.Surface,
    onSurface = Console.Ink,
    surfaceVariant = Console.Grid,
    onSurfaceVariant = Console.InkSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = Console.Dark,
    inverseOnSurface = Console.HeaderInk,
    error = Console.Critical,
    onError = Color.White,
    errorContainer = Console.CriticalSoft,
    onErrorContainer = Console.Critical,
    outline = Console.BorderStrong,
    outlineVariant = Console.Border,
    scrim = Color(0xFF0B0907),
    surfaceBright = Console.Surface,
    surfaceDim = Console.Grid,
    surfaceContainerLowest = Console.Surface,
    surfaceContainerLow = Console.SurfaceAlt,
    surfaceContainer = Console.Surface,
    surfaceContainerHigh = Console.Surface,
    surfaceContainerHighest = Console.Grid
)

private val Sans = FontFamily.SansSerif

private fun style(size: Float, weight: FontWeight, line: Float, spacing: Float = 0f) =
    TextStyle(fontFamily = Sans, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

private val ConsoleTypography = Typography(
    displayLarge = style(40f, FontWeight.SemiBold, 46f, -0.6f),
    displayMedium = style(34f, FontWeight.SemiBold, 40f, -0.5f),
    displaySmall = style(28f, FontWeight.SemiBold, 34f, -0.4f),
    headlineLarge = style(24f, FontWeight.SemiBold, 30f, -0.3f),
    headlineMedium = style(21f, FontWeight.SemiBold, 27f, -0.2f),
    headlineSmall = style(19f, FontWeight.SemiBold, 25f, -0.1f),
    titleLarge = style(17f, FontWeight.SemiBold, 23f),
    titleMedium = style(15f, FontWeight.SemiBold, 21f),
    titleSmall = style(13.5f, FontWeight.SemiBold, 19f),
    bodyLarge = style(15f, FontWeight.Normal, 21f),
    bodyMedium = style(14f, FontWeight.Normal, 20f),
    bodySmall = style(12.5f, FontWeight.Normal, 17f),
    labelLarge = style(13.5f, FontWeight.SemiBold, 18f, 0.1f),
    labelMedium = style(12f, FontWeight.Medium, 16f, 0.2f),
    labelSmall = style(11f, FontWeight.Medium, 14f, 0.3f)
)

private val ConsoleShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)
