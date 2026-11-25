package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorTokensDarkMuted = androidx.compose.ui.graphics.Color(0xFFA1A1AA)

private val TfcLightColorScheme = lightColorScheme(
    primary = TfcCrimson,
    onPrimary = TfcPureWhite,
    primaryContainer = TfcSoftRed,
    onPrimaryContainer = TfcDeepCrimson,
    secondary = TfcJetBlack,
    onSecondary = TfcPureWhite,
    secondaryContainer = TfcCardSurface,
    onSecondaryContainer = TfcJetBlack,
    tertiary = TfcDeepCrimson,
    onTertiary = TfcPureWhite,
    background = TfcWarmWhite,
    onBackground = TfcJetBlack,
    surface = TfcPureWhite,
    onSurface = TfcJetBlack,
    surfaceVariant = TfcCardSurface,
    onSurfaceVariant = TfcMutedText,
    outline = TfcBorderLight,
    error = TfcNonVegRed,
    onError = TfcPureWhite
)

private val TfcDarkColorScheme = darkColorScheme(
    primary = TfcCrimson,
    onPrimary = TfcPureWhite,
    primaryContainer = TfcDarkRedContainer,
    onPrimaryContainer = TfcSoftRed,
    secondary = TfcPureWhite,
    onSecondary = TfcJetBlack,
    secondaryContainer = TfcSurfaceVariantDark,
    onSecondaryContainer = TfcPureWhite,
    tertiary = TfcSoftRed,
    onTertiary = TfcJetBlack,
    background = TfcJetBlack,
    onBackground = TfcPureWhite,
    surface = TfcSurfaceDark,
    onSurface = TfcPureWhite,
    surfaceVariant = TfcSurfaceVariantDark,
    onSurfaceVariant = ColorTokensDarkMuted,
    outline = TfcSurfaceVariantDark,
    error = TfcNonVegRed,
    onError = TfcPureWhite
)

val TfcShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TfcRestaurantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TfcDarkColorScheme else TfcLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TfcTypography,
        shapes = TfcShapes,
        content = content
    )
}
