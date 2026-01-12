package com.example.routinereminder.ui.theme

import androidx.compose.ui.graphics.Color

object AppPalette {
    val SurfaceStrong = Color(0xFF121212)
    val Surface = Color(0xFF1C1C1C)
    val SurfaceAlt = Color(0xFF2A2A2A)
    val SurfaceElevated = Color(0xFF1B1B1B)
    val SurfaceDeep = Color(0xFF0E0E0E)
    val SurfaceOverlay = Color(0xFF101010)
    val SurfaceDialog = Color(0xFF111111)
    val SurfaceMuted = Color(0xFF151515)
    val SurfaceSubtle = Color(0xFF1A1A1A)
    val SurfaceTrack = Color(0xFF3A3A3A)
    val SurfaceDanger = Color(0xFF3B1B1B)

    val TextPrimary = Color(0xFFEAEAEA)
    val TextSecondary = Color(0xFFB0B0B0)
    val TextMuted = Color(0xFF8A8A8A)
    val TextHint = Color(0xFFBBBBBB)
    val TextDisabled = Color(0xFFCBCBCB)
    val TextLight = Color(0xFFE6E6E6)
    val TextSubtle = Color(0xFFBDBDBD)
    val TextSoft = Color(0xFF888888)
    val TextInverse = Color(0xFFFFFFFF)
    val TextAccent = Color(0xFF0FEEE5)

    val Accent = Color(0xFF1E88E5)
    val AccentStrong = Color(0xFF2196F3)
    val AccentSoft = Color(0x332196F3)
    val AccentSecondary = Color(0xFF4CAF50)
    val AccentSecondarySoft = Color(0x334CAF50)
    val AccentOrangeSoft = Color(0x33FF9800)
    val AccentPinkSoft = Color(0x33E91E63)
    val AccentPink = Color(0xFFE91E63)
    val AccentAmberSoft = Color(0x33FFC107)
    val AccentAmber = Color(0xFFFFC107)
    val AccentPurpleSoft = Color(0x33AA00FF)
    val AccentDeepOrangeSoft = Color(0x33FF5722)
    val AccentDeepOrange = Color(0xFFFF5722)
    val AccentCyan = Color(0xFF00BCD4)
    val Info = Color(0xFF64B5F6)

    val Success = Color(0xFF00C853)
    val Danger = Color(0xFFB3261E)
    val DangerSoft = Color(0xFFFFB4B4)
    val DangerAccent = Color(0xFFE53935)
    val DangerBright = Color(0xFFFF5252)

    val BorderSubtle = Color(0xFF333333)
    val BorderNeutral = Color(0xFF9E9E9E)
    val BorderStrong = Color(0xFF424242)

    val MapLocation = Color(0xFF2196F3)
    val MapTrail = Color(0xFFFF4081)
    val MapStroke = Color(0xFFFFFFFF)
    val MapBackground = Color(0xFF000000)

    val Overlay = Color(0x66000000)
    val TagFallbackSoft = Color(0x33444444)
}

// Unified Material 3 Theme Colors based on AppPalette
// Light Theme Colors
val md_theme_light_primary = AppPalette.AccentStrong
val md_theme_light_onPrimary = AppPalette.TextInverse
val md_theme_light_primaryContainer = AppPalette.AccentSoft
val md_theme_light_onPrimaryContainer = AppPalette.TextPrimary
val md_theme_light_secondary = AppPalette.AccentSecondary
val md_theme_light_onSecondary = AppPalette.TextInverse
val md_theme_light_secondaryContainer = AppPalette.AccentSecondarySoft
val md_theme_light_onSecondaryContainer = AppPalette.TextPrimary
val md_theme_light_tertiary = AppPalette.AccentAmber
val md_theme_light_onTertiary = AppPalette.SurfaceStrong
val md_theme_light_tertiaryContainer = AppPalette.AccentAmberSoft
val md_theme_light_onTertiaryContainer = AppPalette.TextPrimary
val md_theme_light_error = AppPalette.Danger
val md_theme_light_onError = AppPalette.TextInverse
val md_theme_light_errorContainer = AppPalette.DangerSoft
val md_theme_light_onErrorContainer = AppPalette.SurfaceStrong
val md_theme_light_outline = AppPalette.BorderNeutral
val md_theme_light_background = AppPalette.Surface
val md_theme_light_onBackground = AppPalette.TextPrimary
val md_theme_light_surface = AppPalette.Surface
val md_theme_light_onSurface = AppPalette.TextPrimary
val md_theme_light_surfaceVariant = AppPalette.SurfaceAlt
val md_theme_light_onSurfaceVariant = AppPalette.TextSecondary
val md_theme_light_inverseSurface = AppPalette.TextPrimary
val md_theme_light_inverseOnSurface = AppPalette.SurfaceStrong
val md_theme_light_inversePrimary = AppPalette.AccentStrong
val md_theme_light_shadow = AppPalette.SurfaceDeep
val md_theme_light_surfaceTint = AppPalette.AccentStrong
val md_theme_light_outlineVariant = AppPalette.BorderStrong
val md_theme_light_scrim = AppPalette.Overlay

// Dark Theme Colors
val md_theme_dark_primary = AppPalette.AccentStrong
val md_theme_dark_onPrimary = AppPalette.TextInverse
val md_theme_dark_primaryContainer = AppPalette.AccentSoft
val md_theme_dark_onPrimaryContainer = AppPalette.TextPrimary
val md_theme_dark_secondary = AppPalette.AccentSecondary
val md_theme_dark_onSecondary = AppPalette.TextInverse
val md_theme_dark_secondaryContainer = AppPalette.AccentSecondarySoft
val md_theme_dark_onSecondaryContainer = AppPalette.TextPrimary
val md_theme_dark_tertiary = AppPalette.AccentAmber
val md_theme_dark_onTertiary = AppPalette.SurfaceStrong
val md_theme_dark_tertiaryContainer = AppPalette.AccentAmberSoft
val md_theme_dark_onTertiaryContainer = AppPalette.TextPrimary
val md_theme_dark_error = AppPalette.Danger
val md_theme_dark_onError = AppPalette.TextInverse
val md_theme_dark_errorContainer = AppPalette.DangerSoft
val md_theme_dark_onErrorContainer = AppPalette.SurfaceStrong
val md_theme_dark_outline = AppPalette.BorderNeutral
val md_theme_dark_background = AppPalette.Surface
val md_theme_dark_onBackground = AppPalette.TextPrimary
val md_theme_dark_surface = AppPalette.Surface
val md_theme_dark_onSurface = AppPalette.TextPrimary
val md_theme_dark_surfaceVariant = AppPalette.SurfaceAlt
val md_theme_dark_onSurfaceVariant = AppPalette.TextSecondary
val md_theme_dark_inverseSurface = AppPalette.TextPrimary
val md_theme_dark_inverseOnSurface = AppPalette.SurfaceStrong
val md_theme_dark_inversePrimary = AppPalette.AccentStrong
val md_theme_dark_shadow = AppPalette.SurfaceDeep
val md_theme_dark_surfaceTint = AppPalette.AccentStrong
val md_theme_dark_outlineVariant = AppPalette.BorderStrong
val md_theme_dark_scrim = AppPalette.Overlay

// Eye Care Theme (Warm) - Example, adjust as needed
val eye_care_warm_primary = Color(0xFF795548) // Brown
val eye_care_warm_onPrimary = Color(0xFFFFFFFF)
val eye_care_warm_surface = Color(0xFFFFF8E1) // Light Yellow/Cream
val eye_care_warm_onSurface = Color(0xFF4E342E) // Dark Brown
val eye_care_warm_background = Color(0xFFFFF3E0) // Light Orange/Cream
val eye_care_warm_onBackground = Color(0xFF4E342E)
