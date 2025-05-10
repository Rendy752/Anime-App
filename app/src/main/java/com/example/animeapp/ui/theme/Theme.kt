package com.example.animeapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.animeapp.utils.ColorUtils

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
    content: @Composable () -> Unit
) {
    val colorScheme = ColorUtils.generateColorScheme(colorStyle, isDarkMode, contrastMode)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}