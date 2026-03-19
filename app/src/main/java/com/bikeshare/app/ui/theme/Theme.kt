package com.bikeshare.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AmoraColorScheme = lightColorScheme(
    primary          = Blue100,
    onPrimary        = White,
    primaryContainer = Blue10,
    onPrimaryContainer = Charcoal,

    secondary        = Blue60,
    onSecondary      = White,

    background       = White,
    onBackground     = Charcoal,

    surface          = Surface,
    onSurface        = Charcoal,
    onSurfaceVariant = SlateGrey,

    error            = Error,
    onError          = White,
)

@Composable
fun BikeshareTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = AmoraColorScheme,
        typography  = Typography,
        content     = content
    )
}
