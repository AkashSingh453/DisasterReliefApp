package com.disasterrelief.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material 3 theme for the Disaster Relief application.
 *
 * Defaults to dark theme for optimal visibility in disaster/low-light conditions.
 * The color scheme uses emergency-appropriate tones: crimsons for urgency,
 * teals for connectivity/status, ambers for warnings.
 */

private val DarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = CrimsonPrimaryDark,
    onPrimaryContainer = Color(0xFFFFDAD6),

    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealSecondaryDark,
    onSecondaryContainer = Color(0xFFA7F3EC),

    tertiary = AmberTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = AmberTertiaryDark,
    onTertiaryContainer = Color(0xFFFFE082),

    background = DarkBackground,
    onBackground = TextOnDark,
    surface = DarkSurface,
    onSurface = TextOnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextOnDarkSecondary,

    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF3C4043),
    outlineVariant = Color(0xFF2C3038)
)

private val LightColorScheme = lightColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = CrimsonPrimaryDark,

    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFA7F3EC),
    onSecondaryContainer = TealSecondaryDark,

    tertiary = AmberTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFE082),
    onTertiaryContainer = AmberTertiaryDark,

    background = LightBackground,
    onBackground = TextOnLight,
    surface = LightSurface,
    onSurface = TextOnLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextOnLightSecondary,

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    outline = Color(0xFF747775),
    outlineVariant = Color(0xFFC4C7C5)
)

@Composable
fun DisasterReliefTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Set status bar color to match the background
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DisasterReliefTypography,
        content = content
    )
}
