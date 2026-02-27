package com.rawen.playlist.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SpotifyDarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = SpotifyBlack,
    primaryContainer = SpotifyGreenDark,
    onPrimaryContainer = SpotifyWhite,
    secondary = SpotifyLightGrey,
    onSecondary = SpotifyBlack,
    secondaryContainer = SpotifySurfaceVariant,
    onSecondaryContainer = SpotifyWhite,
    tertiary = SpotifyGreenLight,
    onTertiary = SpotifyBlack,
    background = SpotifyBlack,
    onBackground = SpotifyWhite,
    surface = SpotifyDarkGrey,
    onSurface = SpotifyWhite,
    surfaceVariant = SpotifySurfaceVariant,
    onSurfaceVariant = SpotifyLightGrey,
    surfaceContainer = SpotifySurface,
    surfaceContainerHigh = SpotifyElevated,
    outline = SpotifyMediumGrey,
    error = SpotifyError,
    onError = SpotifyWhite
)

@Composable
fun PlaylistTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SpotifyDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SpotifyBlack.toArgb()
            window.navigationBarColor = SpotifyBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}