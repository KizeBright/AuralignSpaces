package com.auralign.spaces.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Brand Core ─────────────────────────────────────────────
val BrandPrimary   = Color(0xFF6E1A37) // Maroon
val BrandSecondary = Color(0xFFAE2448) // Crimson
val BrandTertiary  = Color(0xFF72BAA9) // Teal
val BrandAccent    = Color(0xFFD5E7B5) // Sage

// ── Extra accent colors ────────────────────────────────────
val BrandRose      = Color(0xFFE8829B)
val BrandDeepTeal  = Color(0xFF4A9585)
val BrandForest    = Color(0xFF2E7D32)
val BrandGold      = Color(0xFFD4AF37)
val BrandSky       = Color(0xFF88BBD6)
val BrandSlate     = Color(0xFF333D4F)
val BrandNavy      = Color(0xFF1B263B)
val BrandBlueprint = Color(0xFF2196F3)
val BrandCoral     = Color(0xFFE8604A)

// ── Airy Light Palette ────────────────────────────────
val BrandLinen     = Color(0xFFF2EBE3)
val BrandSoftSage  = Color(0xFFE0EADF)
val BrandMist      = Color(0xFFE8ECEF)
val BrandLavender  = Color(0xFFE6E6FA)
val BrandCream     = Color(0xFFFFFDD0)
val BrandIce       = Color(0xFFF0F8FF)

// ── Light Theme Tokens ─────────────────────────────────────
val LightBackground       = Color(0xFFFBF7F8)
val LightSurface          = Color(0xFFFFFFFF)
val LightSurfaceVariant   = Color(0xFFF3EAEC)
val LightSurfaceElevated  = Color(0xFFEEDDE2)
val LightSurfaceContainer = Color(0xFFFAF3F5)
val LightOnBackground     = Color(0xFF1C0B12)
val LightOnSurface        = Color(0xFF1C0B12)
val LightOnSurfaceVariant = Color(0xFF5C4450)
val LightOutline          = Color(0xFFCFB8C0)
val LightOutlineVariant   = Color(0xFFE8D8DC)
val LightOnPrimary        = Color(0xFFFFFFFF)
val LightOnSecondary      = Color(0xFFFFFFFF)
val LightOnTertiary       = Color(0xFF0D2921)
val LightErrorContainer   = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

// ── Dark Theme Tokens ──────────────────────────────────────
val DarkBackground        = Color(0xFF150810)
val DarkSurface           = Color(0xFF221018)
val DarkSurfaceVariant    = Color(0xFF3A1F2A)
val DarkSurfaceElevated   = Color(0xFF4A2535)
val DarkSurfaceContainer  = Color(0xFF2A1520)
val DarkOnBackground      = Color(0xFFF5E8EC)
val DarkOnSurface         = Color(0xFFF5E8EC)
val DarkOnSurfaceVariant  = Color(0xFFCFAFBB)
val DarkOutline           = Color(0xFF7B5566)
val DarkOutlineVariant    = Color(0xFF5A3344)
val DarkOnPrimary         = Color(0xFFFFFFFF)
val DarkOnSecondary       = Color(0xFFFFFFFF)
val DarkOnTertiary        = Color(0xFF002B22)
val DarkPrimary           = Color(0xFFE8829B)
val DarkSecondary         = Color(0xFFE8829B)
val DarkTertiary          = Color(0xFF72BAA9)
val DarkErrorContainer    = Color(0xFF93000A)
val DarkOnErrorContainer  = Color(0xFFFFDAD6)

// ── Utility ──────────────────────────────────────────────────
val ErrorLight   = Color(0xFFBA1A1A)
val ErrorDark    = Color(0xFFFFB4AB)
val SuccessGreen = Color(0xFF2E7D32)
val ErrorRed     = ErrorLight

// ── Gradients ─────────────────────────────────────────────
val PrimaryGradient = Brush.linearGradient(colors = listOf(BrandPrimary, BrandSecondary))
val PremiumGradient = Brush.linearGradient(colors = listOf(BrandPrimary, BrandSecondary, BrandTertiary))
val AccentGradient  = Brush.linearGradient(colors = listOf(BrandTertiary, BrandAccent))
val SplashGradient  = Brush.verticalGradient(colors = listOf(BrandPrimary, BrandSecondary))
val CardGradient    = Brush.linearGradient(colors = listOf(BrandPrimary.copy(0.85f), BrandSecondary.copy(0.85f)))
val GlassGradient   = Brush.verticalGradient(colors = listOf(Color.White.copy(0.15f), Color.White.copy(0.05f)))

// Legacy aliases
val PalettePrimary = BrandPrimary
val PaletteSecondary = BrandSecondary
val PaletteTertiary = BrandTertiary

object AuralignColors {
    val Obsidian = Color(0xFF0A0E1A)
    val Midnight = Color(0xFF111827)
    val Surface = Color(0xFF1C2333)
    val SurfaceCard = Color(0xFF1E2536)
    val Border = Color(0xFF2A3450)
    val Electric = Color(0xFF2563EB)
    val Emerald = Color(0xFF059669)
    val Violet = Color(0xFF7C3AED)
    val Rose = Color(0xFFDC2626)
    val Amber = Color(0xFFD97706)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF475569)
}
