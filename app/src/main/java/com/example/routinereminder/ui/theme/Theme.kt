package com.example.routinereminder.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.example.routinereminder.data.AppThemeColors

private fun darkColors(appThemeColors: AppThemeColors) = darkColorScheme(
    primary = AppPalette.AccentStrong,
    onPrimary = AppPalette.TextInverse,
    primaryContainer = AppPalette.AccentSoft,
    onPrimaryContainer = AppPalette.TextPrimary,
    secondary = AppPalette.AccentSecondary,
    onSecondary = AppPalette.TextInverse,
    secondaryContainer = AppPalette.AccentSecondarySoft,
    onSecondaryContainer = AppPalette.TextPrimary,
    tertiary = AppPalette.AccentAmber,
    onTertiary = AppPalette.SurfaceStrong,
    tertiaryContainer = AppPalette.AccentAmberSoft,
    onTertiaryContainer = AppPalette.TextPrimary,
    error = AppPalette.Danger,
    onError = AppPalette.TextInverse,
    errorContainer = AppPalette.DangerSoft,
    onErrorContainer = AppPalette.SurfaceStrong,
    background = AppPalette.Surface,
    onBackground = AppPalette.TextPrimary,
    surface = AppPalette.Surface,
    onSurface = AppPalette.TextPrimary,
    surfaceVariant = AppPalette.SurfaceAlt,
    onSurfaceVariant = AppPalette.TextSecondary,
    outline = AppPalette.BorderNeutral,
    inverseSurface = AppPalette.TextPrimary,
    inverseOnSurface = AppPalette.SurfaceStrong,
    inversePrimary = AppPalette.AccentStrong,
    surfaceTint = AppPalette.AccentStrong,
    outlineVariant = AppPalette.BorderStrong,
    scrim = AppPalette.Overlay
)

@Composable
fun RoutineReminderTheme(
    appThemeColors: AppThemeColors = AppThemeColors.Default,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(appThemeColors) {
        AppPalette.updateFromTheme(appThemeColors)
        darkColors(appThemeColors)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    val configuration = LocalConfiguration.current
    val baseDensity = LocalDensity.current
    val screenScale = (configuration.screenWidthDp / 411f).coerceIn(0.75f, 1f)
    val scaledDensity = Density(
        density = baseDensity.density * screenScale,
        fontScale = baseDensity.fontScale * screenScale
    )

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography, // Will create Typography.kt next
            content = content
        )
    }
}
