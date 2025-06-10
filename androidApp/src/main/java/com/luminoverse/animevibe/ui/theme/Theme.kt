package com.luminoverse.animevibe.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import com.luminoverse.animevibe.utils.ColorUtils

enum class ContrastMode {
    Normal, Medium, High
}

enum class ColorStyle {
    Default, Vibrant, Monochrome
}

@Composable
fun AppTheme(
    isDarkMode: Boolean = false,
    contrastMode: ContrastMode = ContrastMode.Normal,
    colorStyle: ColorStyle = ColorStyle.Default,
    isRtl: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = ColorUtils.generateColorScheme(colorStyle, isDarkMode, contrastMode)
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val view = LocalView.current
    if (!view.isInEditMode) {
        @Suppress("DEPRECATION")
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !isDarkMode
            windowInsetsController.isAppearanceLightNavigationBars = !isDarkMode
        }
    }
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}