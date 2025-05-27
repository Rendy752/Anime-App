package com.luminoverse.animevibe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}