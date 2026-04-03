package com.example.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = White,
    primaryContainer = BlueContainer,
    onPrimaryContainer = OnBlueContainer,
    secondary = TealAccent,
    onSecondary = White,
    secondaryContainer = TealLight,
    onSecondaryContainer = DarkGray,
    background = OffWhite,
    onBackground = DarkGray,
    surface = White,
    onSurface = DarkGray,
    surfaceVariant = LightGray,
    onSurfaceVariant = DarkGray,
    error = RedEmergency,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueAccent,
    onPrimary = DarkBackground,
    primaryContainer = BlueDark,
    onPrimaryContainer = BlueContainer,
    secondary = TealAccent,
    onSecondary = DarkBackground,
    secondaryContainer = DarkCard,
    onSecondaryContainer = TealLight,
    background = DarkBackground,
    onBackground = White,
    surface = DarkSurface,
    onSurface = White,
    surfaceVariant = DarkCard,
    onSurfaceVariant = MediumGray,
    error = RedEmergency,
    onError = White
)

@Composable
fun BusTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) DarkBackground.toArgb() else BluePrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
