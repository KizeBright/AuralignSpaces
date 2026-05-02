package com.auralign.spaces.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary              = DarkPrimary,          // BrandRose
    onPrimary            = BrandNavy,
    primaryContainer     = BrandPrimary,         // Deep Maroon impact
    onPrimaryContainer   = DarkOnBackground,
    secondary            = BrandDeepTeal,        // Introduce Teal balance
    onSecondary          = BrandNavy,
    secondaryContainer   = BrandSlate,           // Surface depth
    onSecondaryContainer = BrandSky,
    tertiary             = BrandTertiary,        // Sage
    onTertiary           = BrandNavy,
    tertiaryContainer    = DarkSurfaceVariant,
    onTertiaryContainer  = BrandAccent,
    background           = DarkBackground,       // Deep Navy/Slate
    onBackground         = DarkOnBackground,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVariant,
    surfaceTint          = BrandRose.copy(alpha = 0.1f),
    outline              = DarkOutline,
    outlineVariant       = DarkOutlineVariant,
    error                = ErrorDark,
    onError              = DarkBackground,
    errorContainer       = DarkErrorContainer,
    onErrorContainer     = DarkOnErrorContainer,
    inverseSurface       = DarkOnBackground,
    inverseOnSurface     = DarkBackground,
    inversePrimary       = BrandPrimary
)

private val LightColorScheme = lightColorScheme(
    primary              = BrandPrimary,
    onPrimary            = LightOnPrimary,
    primaryContainer     = BrandSoftSage,
    onPrimaryContainer   = BrandPrimary,
    secondary            = BrandSecondary,
    onSecondary          = LightOnSecondary,
    secondaryContainer   = BrandMist,
    onSecondaryContainer = BrandSecondary,
    tertiary             = BrandTertiary,
    onTertiary           = LightOnTertiary,
    tertiaryContainer    = BrandAccent,
    onTertiaryContainer  = LightOnTertiary,
    background           = BrandIce,             // Airy Background
    onBackground         = LightOnBackground,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = BrandMist,
    onSurfaceVariant     = LightOnSurfaceVariant,
    surfaceTint          = BrandPrimary.copy(alpha = 0.05f),
    outline              = LightOutline,
    outlineVariant       = LightOutlineVariant,
    error                = ErrorLight,
    onError              = LightOnPrimary,
    errorContainer       = LightErrorContainer,
    onErrorContainer     = LightOnErrorContainer,
    inverseSurface       = LightOnBackground,
    inverseOnSurface     = LightBackground,
    inversePrimary       = BrandRose
)

@Composable
fun AuralignTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
