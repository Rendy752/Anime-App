package com.example.animeapp.utils

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.animeapp.ui.theme.ColorStyle
import com.example.animeapp.ui.theme.ContrastMode
import kotlin.math.abs

data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

object ColorUtils {
    // Base colors for Default style
    private val basePrimaryLight = Color(0xFF005FAF) // Blue
    private val basePrimaryDark = Color(0xFFA5C8FF)
    private val baseSecondaryLight = Color(0xFF495F81) // Blue-gray
    private val baseSecondaryDark = Color(0xFFB1C8EE)
    private val baseTertiaryLight = Color(0xFF874296) // Purple
    private val baseTertiaryDark = Color(0xFFF4AEFF)
    private val baseErrorLight = Color(0xFFBA1A1A) // Red
    private val baseErrorDark = Color(0xFFFFB4AB)

    // Base colors for Vibrant style
    private val vibrantPrimaryLight = Color(0xFF008080) // Teal
    private val vibrantPrimaryDark = Color(0xFF80FFFF)
    private val vibrantSecondaryLight = Color(0xFF7B3F00) // Coral
    private val vibrantSecondaryDark = Color(0xFFFFB280)
    private val vibrantTertiaryLight = Color(0xFF5E8C31) // Lime
    private val vibrantTertiaryDark = Color(0xFFB3D68C)
    private val vibrantErrorLight = Color(0xFFB0006B) // Magenta
    private val vibrantErrorDark = Color(0xFFFF80C1)

    // Base colors for Monochrome style
    private val monoPrimaryLight = Color(0xFF666666) // Medium gray
    private val monoPrimaryDark = Color(0xFFB3B3B3)
    private val monoSecondaryLight = Color(0xFF4D4D4D) // Darker gray
    private val monoSecondaryDark = Color(0xFFCCCCCC)
    private val monoTertiaryLight = Color(0xFF808080) // Lighter gray
    private val monoTertiaryDark = Color(0xFFD9D9D9)
    private val monoErrorLight = Color(0xFF595959) // Slightly darker gray
    private val monoErrorDark = Color(0xFFBFBFBF)

    // Convert Color to HSL for manipulation
    private fun Color.toHSL(): Triple<Float, Float, Float> {
        val r = red / 255f
        val g = green / 255f
        val b = blue / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f
        val s: Float
        val h: Float

        if (max == min) {
            h = 0f
            s = 0f
        } else {
            val d = max - min
            s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
            h = when (max) {
                r -> (g - b) / d + (if (g < b) 6f else 0f)
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            } / 6f
        }
        return Triple(h.coerceIn(0f, 1f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
    }

    // Convert HSL back to Color
    private fun hslToColor(h: Float, s: Float, l: Float): Color {
        val hClamped = h.coerceIn(0f, 1f)
        val sClamped = s.coerceIn(0f, 1f)
        val lClamped = l.coerceIn(0f, 1f)
        val c = (1f - abs(2f * lClamped - 1f)) * sClamped
        val x = c * (1f - abs((hClamped * 6f) % 2f - 1f))
        val m = lClamped - c / 2f
        var r: Float
        var g: Float
        var b: Float

        when ((hClamped * 6f).toInt()) {
            0 -> { r = c; g = x; b = 0f }
            1 -> { r = x; g = c; b = 0f }
            2 -> { r = 0f; g = c; b = x }
            3 -> { r = 0f; g = x; b = c }
            4 -> { r = x; g = 0f; b = c }
            else -> { r = c; g = 0f; b = x }
        }

        r = (r + m).coerceIn(0f, 1f)
        g = (g + m).coerceIn(0f, 1f)
        b = (b + m).coerceIn(0f, 1f)
        return Color((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    // Adjust lightness by adding/subtracting
    private fun adjustLightness(color: Color, delta: Float): Color {
        val (h, s, l) = color.toHSL()
        return hslToColor(h, s, (l + delta).coerceIn(0f, 1f))
    }

    // Generate ColorScheme for a given style, mode, and contrast
    fun generateColorScheme(
        style: ColorStyle,
        isDark: Boolean,
        contrast: ContrastMode
    ): ColorScheme {
        // Base colors depend on light/dark mode and style
        val baseColors = when (style) {
            ColorStyle.Default -> mapOf(
                "primary" to if (isDark) basePrimaryDark else basePrimaryLight,
                "secondary" to if (isDark) baseSecondaryDark else baseSecondaryLight,
                "tertiary" to if (isDark) baseTertiaryDark else baseTertiaryLight,
                "error" to if (isDark) baseErrorDark else baseErrorLight
            )
            ColorStyle.Vibrant -> mapOf(
                "primary" to if (isDark) vibrantPrimaryDark else vibrantPrimaryLight,
                "secondary" to if (isDark) vibrantSecondaryDark else vibrantSecondaryLight,
                "tertiary" to if (isDark) vibrantTertiaryDark else vibrantTertiaryLight,
                "error" to if (isDark) vibrantErrorDark else vibrantErrorLight
            )
            ColorStyle.Monochrome -> mapOf(
                "primary" to if (isDark) monoPrimaryDark else monoPrimaryLight,
                "secondary" to if (isDark) monoSecondaryDark else monoSecondaryLight,
                "tertiary" to if (isDark) monoTertiaryDark else monoTertiaryLight,
                "error" to if (isDark) monoErrorDark else monoErrorLight
            )
        }

        val primary = baseColors["primary"]!!
        val secondary = baseColors["secondary"]!!
        val tertiary = baseColors["tertiary"]!!
        val error = baseColors["error"]!!

        // Background and surface colors from Theme.kt
        val (background, onBackground, surface, onSurface) = if (isDark) {
            Quad(
                Color(0xFF101319), // backgroundDark
                Color(0xFFE1E2EA), // onBackgroundDark
                Color(0xFF101319), // surfaceDark
                Color(0xFFE1E2EA)  // onSurfaceDark
            )
        } else {
            Quad(
                Color(0xFFF9F9FF), // backgroundLight
                Color(0xFF191C21), // onBackgroundLight
                Color(0xFFF9F9FF), // surfaceLight
                Color(0xFF191C21)  // onSurfaceLight
            )
        }

        // Define ColorScheme based on style and contrast mode
        return if (isDark) {
            when (style) {
                ColorStyle.Default -> when (contrast) {
                    ContrastMode.Normal -> darkColorScheme(
                        primary = primary,
                        onPrimary = Color(0xFF00315F),
                        primaryContainer = Color(0xFF62A4FB),
                        onPrimaryContainer = Color(0xFF00396D),
                        secondary = secondary,
                        onSecondary = Color(0xFF1A3150),
                        secondaryContainer = Color(0xFF314868),
                        onSecondaryContainer = Color(0xFFA0B6DC),
                        tertiary = tertiary,
                        onTertiary = Color(0xFF530B64),
                        tertiaryContainer = Color(0xFFD286E0),
                        onTertiaryContainer = Color(0xFF5C186D),
                        error = error,
                        onError = Color(0xFF690005),
                        errorContainer = Color(0xFF93000A),
                        onErrorContainer = Color(0xFFFFDAD6),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = onSurface,
                        surfaceVariant = Color(0xFF414752),
                        onSurfaceVariant = Color(0xFFC1C6D3),
                        outline = Color(0xFF8B919D),
                        outlineVariant = Color(0xFF414752),
                        scrim = Color(0xFF000000),
                        inverseSurface = Color(0xFFE1E2EA),
                        inverseOnSurface = Color(0xFF2E3036),
                        inversePrimary = Color(0xFF005FAF)
                    )
                    ContrastMode.Medium -> darkColorScheme(
                        primary = Color(0xFFCADDFF),
                        onPrimary = Color(0xFF00264C),
                        primaryContainer = Color(0xFF62A4FB),
                        onPrimaryContainer = Color(0xFF001732),
                        secondary = Color(0xFFCADDFF),
                        onSecondary = Color(0xFF0D2644),
                        secondaryContainer = Color(0xFF7C92B6),
                        onSecondaryContainer = Color(0xFF000000),
                        tertiary = Color(0xFFFBCEFF),
                        onTertiary = Color(0xFF440055),
                        tertiaryContainer = Color(0xFFD286E0),
                        onTertiaryContainer = Color(0xFF2C0038),
                        error = Color(0xFFFFD2CC),
                        onError = Color(0xFF540003),
                        errorContainer = Color(0xFFFF5449),
                        onErrorContainer = Color(0xFF000000),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = Color(0xFFFFFFFF),
                        surfaceVariant = Color(0xFF414752),
                        onSurfaceVariant = Color(0xFFD7DCE9),
                        outline = Color(0xFFACB2BF),
                        outlineVariant = Color(0xFF8B909C),
                        scrim = Color(0xFF000000),
                        inverseSurface = Color(0xFFE1E2EA),
                        inverseOnSurface = Color(0xFF272A30),
                        inversePrimary = Color(0xFF004888)
                    )
                    ContrastMode.High -> darkColorScheme(
                        primary = Color(0xFFEAF0FF),
                        onPrimary = Color(0xFF000000),
                        primaryContainer = Color(0xFF9FC4FF),
                        onPrimaryContainer = Color(0xFF000B1E),
                        secondary = Color(0xFFEAF0FF),
                        onSecondary = Color(0xFF000000),
                        secondaryContainer = Color(0xFFADC4EA),
                        onSecondaryContainer = Color(0xFF000B1E),
                        tertiary = Color(0xFFFFEAFD),
                        onTertiary = Color(0xFF000000),
                        tertiaryContainer = Color(0xFFF3A8FF),
                        onTertiaryContainer = Color(0xFF1A0022),
                        error = Color(0xFFFFECE9),
                        onError = Color(0xFF000000),
                        errorContainer = Color(0xFFFFAEA4),
                        onErrorContainer = Color(0xFF220001),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = Color(0xFFFFFFFF),
                        surfaceVariant = Color(0xFF414752),
                        onSurfaceVariant = Color(0xFFFFFFFF),
                        outline = Color(0xFFEBF0FD),
                        outlineVariant = Color(0xFFBDC3CF),
                        scrim = Color(0xFF000000),
                        inverseSurface = Color(0xFFE1E2EA),
                        inverseOnSurface = Color(0xFF000000),
                        inversePrimary = Color(0xFF004888)
                    )
                }
                ColorStyle.Vibrant -> when (contrast) {
                    ContrastMode.Normal -> darkColorScheme(
                        primary = adjustLightness(primary, 0.1f),
                        onPrimary = adjustLightness(primary, -0.3f),
                        primaryContainer = adjustLightness(primary, 0.3f),
                        onPrimaryContainer = adjustLightness(primary, -0.2f),
                        secondary = adjustLightness(secondary, 0.1f),
                        onSecondary = adjustLightness(secondary, -0.3f),
                        secondaryContainer = adjustLightness(secondary, 0.3f),
                        onSecondaryContainer = adjustLightness(secondary, -0.2f),
                        tertiary = adjustLightness(tertiary, 0.1f),
                        onTertiary = adjustLightness(tertiary, -0.3f),
                        tertiaryContainer = adjustLightness(tertiary, 0.3f),
                        onTertiaryContainer = adjustLightness(tertiary, -0.2f),
                        error = adjustLightness(error, 0.1f),
                        onError = adjustLightness(error, -0.3f),
                        errorContainer = adjustLightness(error, 0.3f),
                        onErrorContainer = adjustLightness(error, -0.2f),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = onSurface,
                        surfaceVariant = adjustLightness(secondary, -0.1f),
                        onSurfaceVariant = adjustLightness(secondary, 0.4f),
                        outline = adjustLightness(secondary, 0.2f),
                        outlineVariant = adjustLightness(secondary, -0.2f),
                        scrim = Color(0xFF000000),
                        inverseSurface = adjustLightness(primary, -0.5f),
                        inverseOnSurface = adjustLightness(primary, 0.4f),
                        inversePrimary = adjustLightness(primary, -0.4f)
                    )
                    ContrastMode.Medium -> darkColorScheme(
                        primary = adjustLightness(primary, 0.2f),
                        onPrimary = adjustLightness(primary, -0.4f),
                        primaryContainer = adjustLightness(primary, 0.4f),
                        onPrimaryContainer = adjustLightness(primary, -0.3f),
                        secondary = adjustLightness(secondary, 0.2f),
                        onSecondary = adjustLightness(secondary, -0.4f),
                        secondaryContainer = adjustLightness(secondary, 0.4f),
                        onSecondaryContainer = adjustLightness(secondary, -0.3f),
                        tertiary = adjustLightness(tertiary, 0.2f),
                        onTertiary = adjustLightness(tertiary, -0.4f),
                        tertiaryContainer = adjustLightness(tertiary, 0.4f),
                        onTertiaryContainer = adjustLightness(tertiary, -0.3f),
                        error = adjustLightness(error, 0.2f),
                        onError = adjustLightness(error, -0.4f),
                        errorContainer = adjustLightness(error, 0.4f),
                        onErrorContainer = adjustLightness(error, -0.3f),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = Color(0xFFFFFFFF),
                        surfaceVariant = adjustLightness(secondary, -0.05f),
                        onSurfaceVariant = adjustLightness(secondary, 0.5f),
                        outline = adjustLightness(secondary, 0.3f),
                        outlineVariant = adjustLightness(secondary, -0.1f),
                        scrim = Color(0xFF000000),
                        inverseSurface = adjustLightness(primary, -0.6f),
                        inverseOnSurface = adjustLightness(primary, 0.5f),
                        inversePrimary = adjustLightness(primary, -0.5f)
                    )
                    ContrastMode.High -> darkColorScheme(
                        primary = adjustLightness(primary, 0.3f),
                        onPrimary = Color(0xFF000000),
                        primaryContainer = adjustLightness(primary, 0.5f),
                        onPrimaryContainer = Color(0xFF000000),
                        secondary = adjustLightness(secondary, 0.3f),
                        onSecondary = Color(0xFF000000),
                        secondaryContainer = adjustLightness(secondary, 0.5f),
                        onSecondaryContainer = Color(0xFF000000),
                        tertiary = adjustLightness(tertiary, 0.3f),
                        onTertiary = Color(0xFF000000),
                        tertiaryContainer = adjustLightness(tertiary, 0.5f),
                        onTertiaryContainer = Color(0xFF000000),
                        error = adjustLightness(error, 0.3f),
                        onError = Color(0xFF000000),
                        errorContainer = adjustLightness(error, 0.5f),
                        onErrorContainer = Color(0xFF000000),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = Color(0xFFFFFFFF),
                        surfaceVariant = adjustLightness(secondary, 0.0f),
                        onSurfaceVariant = Color(0xFFFFFFFF),
                        outline = adjustLightness(secondary, 0.4f),
                        outlineVariant = adjustLightness(secondary, 0.1f),
                        scrim = Color(0xFF000000),
                        inverseSurface = adjustLightness(primary, -0.7f),
                        inverseOnSurface = Color(0xFFFFFFFF),
                        inversePrimary = adjustLightness(primary, -0.6f)
                    )
                }
                ColorStyle.Monochrome -> when (contrast) {
                    ContrastMode.Normal -> darkColorScheme(
                        primary = adjustLightness(primary, 0.1f),
                        onPrimary = adjustLightness(primary, -0.2f),
                        primaryContainer = adjustLightness(primary, 0.3f),
                        onPrimaryContainer = adjustLightness(primary, -0.1f),
                        secondary = adjustLightness(secondary, 0.1f),
                        onSecondary = adjustLightness(secondary, -0.2f),
                        secondaryContainer = adjustLightness(secondary, 0.3f),
                        onSecondaryContainer = adjustLightness(secondary, -0.1f),
                        tertiary = adjustLightness(tertiary, 0.1f),
                        onTertiary = adjustLightness(tertiary, -0.2f),
                        tertiaryContainer = adjustLightness(tertiary, 0.3f),
                        onTertiaryContainer = adjustLightness(tertiary, -0.1f),
                        error = adjustLightness(error, 0.1f),
                        onError = adjustLightness(error, -0.2f),
                        errorContainer = adjustLightness(error, 0.3f),
                        onErrorContainer = adjustLightness(error, -0.1f),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = onSurface,
                        surfaceVariant = adjustLightness(secondary, -0.1f),
                        onSurfaceVariant = adjustLightness(secondary, 0.4f),
                        outline = adjustLightness(secondary, 0.2f),
                        outlineVariant = adjustLightness(secondary, -0.2f),
                        scrim = Color(0xFF000000),
                        inverseSurface = adjustLightness(primary, -0.5f),
                        inverseOnSurface = adjustLightness(primary, 0.4f),
                        inversePrimary = adjustLightness(primary, -0.3f)
                    )
                    ContrastMode.Medium -> darkColorScheme(
                        primary = adjustLightness(primary, 0.15f),
                        onPrimary = adjustLightness(primary, -0.3f),
                        primaryContainer = adjustLightness(primary, 0.35f),
                        onPrimaryContainer = adjustLightness(primary, -0.2f),
                        secondary = adjustLightness(secondary, 0.15f),
                        onSecondary = adjustLightness(secondary, -0.3f),
                        secondaryContainer = adjustLightness(secondary, 0.35f),
                        onSecondaryContainer = adjustLightness(secondary, -0.2f),
                        tertiary = adjustLightness(tertiary, 0.15f),
                        onTertiary = adjustLightness(tertiary, -0.3f),
                        tertiaryContainer = adjustLightness(tertiary, 0.35f),
                        onTertiaryContainer = adjustLightness(tertiary, -0.2f),
                        error = adjustLightness(error, 0.15f),
                        onError = adjustLightness(error, -0.3f),
                        errorContainer = adjustLightness(error, 0.35f),
                        onErrorContainer = adjustLightness(error, -0.2f),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = Color(0xFFFFFFFF),
                        surfaceVariant = adjustLightness(secondary, -0.05f),
                        onSurfaceVariant = adjustLightness(secondary, 0.5f),
                        outline = adjustLightness(secondary, 0.3f),
                        outlineVariant = adjustLightness(secondary, -0.1f),
                        scrim = Color(0xFF000000),
                        inverseSurface = adjustLightness(primary, -0.6f),
                        inverseOnSurface = adjustLightness(primary, 0.5f),
                        inversePrimary = adjustLightness(primary, -0.4f)
                    )
                    ContrastMode.High -> darkColorScheme(
                        primary = adjustLightness(primary, 0.2f),
                        onPrimary = Color(0xFF000000),
                        primaryContainer = adjustLightness(primary, 0.4f),
                        onPrimaryContainer = Color(0xFF000000),
                        secondary = adjustLightness(secondary, 0.2f),
                        onSecondary = Color(0xFF000000),
                        secondaryContainer = adjustLightness(secondary, 0.4f),
                        onSecondaryContainer = Color(0xFF000000),
                        tertiary = adjustLightness(tertiary, 0.2f),
                        onTertiary = Color(0xFF000000),
                        tertiaryContainer = adjustLightness(tertiary, 0.4f),
                        onTertiaryContainer = Color(0xFF000000),
                        error = adjustLightness(error, 0.2f),
                        onError = Color(0xFF000000),
                        errorContainer = adjustLightness(error, 0.4f),
                        onErrorContainer = Color(0xFF000000),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = Color(0xFFFFFFFF),
                        surfaceVariant = adjustLightness(secondary, 0.0f),
                        onSurfaceVariant = Color(0xFFFFFFFF),
                        outline = adjustLightness(secondary, 0.4f),
                        outlineVariant = adjustLightness(secondary, 0.1f),
                        scrim = Color(0xFF000000),
                        inverseSurface = adjustLightness(primary, -0.7f),
                        inverseOnSurface = Color(0xFFFFFFFF),
                        inversePrimary = adjustLightness(primary, -0.5f)
                    )
                }
            }.copy(
                surfaceDim = Color(0xFF101319),
                surfaceBright = Color(0xFF36393F),
                surfaceContainerLowest = Color(0xFF0B0E13),
                surfaceContainerLow = Color(0xFF191C21),
                surfaceContainer = Color(0xFF1D2025),
                surfaceContainerHigh = Color(0xFF272A30),
                surfaceContainerHighest = Color(0xFF32353B)
            )
        } else {
            when (style) {
                ColorStyle.Default -> when (contrast) {
                    ContrastMode.Normal -> lightColorScheme(
                        primary = primary,
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFF62A4FB),
                        onPrimaryContainer = Color(0xFF00396D),
                        secondary = secondary,
                        onSecondary = Color(0xFFFFFFFF),
                        secondaryContainer = Color(0xFFC2D8FF),
                        onSecondaryContainer = Color(0xFF485E80),
                        tertiary = tertiary,
                        onTertiary = Color(0xFFFFFFFF),
                        tertiaryContainer = Color(0xFFD286E0),
                        onTertiaryContainer = Color(0xFF5C186D),
                        error = error,
                        onError = Color(0xFFFFFFFF),
                        errorContainer = Color(0xFFFFDAD6),
                        onErrorContainer = Color(0xFF93000A),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = onSurface,
                        surfaceVariant = Color(0xFFDDE2F0),
                        onSurfaceVariant = Color(0xFF414752),
                        outline = Color(0xFF727783),
                        outlineVariant = Color(0xFFC1C6D3),
                        scrim = Color(0xFF000000),
                        inverseSurface = Color(0xFF2E3036),
                        inverseOnSurface = Color(0xFFEFF0F8),
                        inversePrimary = Color(0xFFA5C8FF)
                    )
                    ContrastMode.Medium -> lightColorScheme(
                        primary = Color(0xFF003669),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFF1E6EC1),
                        onPrimaryContainer = Color(0xFFFFFFFF),
                        secondary = Color(0xFF203756),
                        onSecondary = Color(0xFFFFFFFF),
                        secondaryContainer = Color(0xFF586E90),
                        onSecondaryContainer = Color(0xFFFFFFFF),
                        tertiary = Color(0xFF59146A),
                        onTertiary = Color(0xFFFFFFFF),
                        tertiaryContainer = Color(0xFF9751A6),
                        onTertiaryContainer = Color(0xFFFFFFFF),
                        error = Color(0xFF740006),
                        onError = Color(0xFFFFFFFF),
                        errorContainer = Color(0xFFCF2C27),
                        onErrorContainer = Color(0xFFFFFFFF),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = Color(0xFF0E1116),
                        surfaceVariant = Color(0xFFDDE2F0),
                        onSurfaceVariant = Color(0xFF313641),
                        outline = Color(0xFF4D535D),
                        outlineVariant = Color(0xFF686D79),
                        scrim = Color(0xFF000000),
                        inverseSurface = Color(0xFF2E3036),
                        inverseOnSurface = Color(0xFFEFF0F8),
                        inversePrimary = Color(0xFFA5C8FF)
                    )
                    ContrastMode.High -> lightColorScheme(
                        primary = Color(0xFF002C57),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFF004A8A),
                        onPrimaryContainer = Color(0xFFFFFFFF),
                        secondary = Color(0xFF152D4B),
                        onSecondary = Color(0xFFFFFFFF),
                        secondaryContainer = Color(0xFF344A6A),
                        onSecondaryContainer = Color(0xFFFFFFFF),
                        tertiary = Color(0xFF4E045F),
                        onTertiary = Color(0xFFFFFFFF),
                        tertiaryContainer = Color(0xFF6F2B7F),
                        onTertiaryContainer = Color(0xFFFFFFFF),
                        error = Color(0xFF600004),
                        onError = Color(0xFFFFFFFF),
                        errorContainer = Color(0xFF98000A),
                        onErrorContainer = Color(0xFFFFFFFF),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = Color(0xFF000000),
                        surfaceVariant = Color(0xFFDDE2F0),
                        onSurfaceVariant = Color(0xFF000000),
                        outline = Color(0xFF272C36),
                        outlineVariant = Color(0xFF444954),
                        scrim = Color(0xFF000000),
                        inverseSurface = Color(0xFF2E3036),
                        inverseOnSurface = Color(0xFFFFFFFF),
                        inversePrimary = Color(0xFFA5C8FF)
                    )
                }
                ColorStyle.Vibrant -> when (contrast) {
                    ContrastMode.Normal -> lightColorScheme(
                        primary = primary,
                        onPrimary = adjustLightness(primary, -0.3f),
                        primaryContainer = adjustLightness(primary, 0.3f),
                        onPrimaryContainer = adjustLightness(primary, -0.2f),
                        secondary = secondary,
                        onSecondary = adjustLightness(secondary, -0.3f),
                        secondaryContainer = adjustLightness(secondary, 0.3f),
                        onSecondaryContainer = adjustLightness(secondary, -0.2f),
                        tertiary = tertiary,
                        onTertiary = adjustLightness(tertiary, -0.3f),
                        tertiaryContainer = adjustLightness(tertiary, 0.3f),
                        onTertiaryContainer = adjustLightness(tertiary, -0.2f),
                        error = error,
                        onError = adjustLightness(error, -0.3f),
                        errorContainer = adjustLightness(error, 0.3f),
                        onErrorContainer = adjustLightness(error, -0.2f),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = onSurface,
                        surfaceVariant = adjustLightness(secondary, -0.1f),
                        onSurfaceVariant = adjustLightness(secondary, 0.4f),
                        outline = adjustLightness(secondary, 0.2f),
                        outlineVariant = adjustLightness(secondary, -0.2f),
                        scrim = Color(0xFF000000),
                        inverseSurface = adjustLightness(primary, 0.5f),
                        inverseOnSurface = adjustLightness(primary, -0.4f),
                        inversePrimary = adjustLightness(primary, 0.4f)
                    )
                    ContrastMode.Medium -> lightColorScheme(
                        primary = adjustLightness(primary, -0.1f),
                        onPrimary = adjustLightness(primary, -0.4f),
                        primaryContainer = adjustLightness(primary, 0.2f),
                        onPrimaryContainer = adjustLightness(primary, -0.3f),
                        secondary = adjustLightness(secondary, -0.1f),
                        onSecondary = adjustLightness(secondary, -0.4f),
                        secondaryContainer = adjustLightness(secondary, 0.2f),
                        onSecondaryContainer = adjustLightness(secondary, -0.3f),
                        tertiary = adjustLightness(tertiary, -0.1f),
                        onTertiary = adjustLightness(tertiary, -0.4f),
                        tertiaryContainer = adjustLightness(tertiary, 0.2f),
                        onTertiaryContainer = adjustLightness(tertiary, -0.3f),
                        error = adjustLightness(error, -0.1f),
                        onError = adjustLightness(error, -0.4f),
                        errorContainer = adjustLightness(error, 0.2f),
                        onErrorContainer = adjustLightness(error, -0.3f),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = Color(0xFF0E1116),
                        surfaceVariant = adjustLightness(secondary, -0.05f),
                        onSurfaceVariant = adjustLightness(secondary, 0.5f),
                        outline = adjustLightness(secondary, 0.3f),
                        outlineVariant = adjustLightness(secondary, -0.1f),
                        scrim = Color(0xFF000000),
                        inverseSurface = adjustLightness(primary, 0.6f),
                        inverseOnSurface = adjustLightness(primary, -0.5f),
                        inversePrimary = adjustLightness(primary, 0.3f)
                    )
                    ContrastMode.High -> lightColorScheme(
                        primary = adjustLightness(primary, -0.2f),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = adjustLightness(primary, 0.1f),
                        onPrimaryContainer = Color(0xFFFFFFFF),
                        secondary = adjustLightness(secondary, -0.2f),
                        onSecondary = Color(0xFFFFFFFF),
                        secondaryContainer = adjustLightness(secondary, 0.1f),
                        onSecondaryContainer = Color(0xFFFFFFFF),
                        tertiary = adjustLightness(tertiary, -0.2f),
                        onTertiary = Color(0xFFFFFFFF),
                        tertiaryContainer = adjustLightness(tertiary, 0.1f),
                        onTertiaryContainer = Color(0xFFFFFFFF),
                        error = adjustLightness(error, -0.2f),
                        onError = Color(0xFFFFFFFF),
                        errorContainer = adjustLightness(error, 0.1f),
                        onErrorContainer = Color(0xFFFFFFFF),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = Color(0xFF000000),
                        surfaceVariant = adjustLightness(secondary, 0.0f),
                        onSurfaceVariant = Color(0xFF000000),
                        outline = adjustLightness(secondary, 0.4f),
                        outlineVariant = adjustLightness(secondary, 0.1f),
                        scrim = Color(0xFF000000),
                        inverseSurface = adjustLightness(primary, 0.7f),
                        inverseOnSurface = Color(0xFF000000),
                        inversePrimary = adjustLightness(primary, 0.2f)
                    )
                }
                ColorStyle.Monochrome -> when (contrast) {
                    ContrastMode.Normal -> lightColorScheme(
                        primary = primary,
                        onPrimary = adjustLightness(primary, -0.3f),
                        primaryContainer = adjustLightness(primary, 0.3f),
                        onPrimaryContainer = adjustLightness(primary, -0.2f),
                        secondary = secondary,
                        onSecondary = adjustLightness(secondary, -0.3f),
                        secondaryContainer = adjustLightness(secondary, 0.3f),
                        onSecondaryContainer = adjustLightness(secondary, -0.2f),
                        tertiary = tertiary,
                        onTertiary = adjustLightness(tertiary, -0.3f),
                        tertiaryContainer = adjustLightness(tertiary, 0.3f),
                        onTertiaryContainer = adjustLightness(tertiary, -0.2f),
                        error = error,
                        onError = adjustLightness(error, -0.3f),
                        errorContainer = adjustLightness(error, 0.3f),
                        onErrorContainer = adjustLightness(error, -0.2f),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = onSurface,
                        surfaceVariant = adjustLightness(secondary, -0.1f),
                        onSurfaceVariant = adjustLightness(secondary, 0.4f),
                        outline = adjustLightness(secondary, 0.2f),
                        outlineVariant = adjustLightness(secondary, -0.2f),
                        scrim = Color(0xFF000000),
                        inverseSurface = adjustLightness(primary, 0.5f),
                        inverseOnSurface = adjustLightness(primary, -0.4f),
                        inversePrimary = adjustLightness(primary, 0.4f)
                    )
                    ContrastMode.Medium -> lightColorScheme(
                        primary = adjustLightness(primary, -0.1f),
                        onPrimary = adjustLightness(primary, -0.4f),
                        primaryContainer = adjustLightness(primary, 0.25f),
                        onPrimaryContainer = adjustLightness(primary, -0.3f),
                        secondary = adjustLightness(secondary, -0.1f),
                        onSecondary = adjustLightness(secondary, -0.4f),
                        secondaryContainer = adjustLightness(secondary, 0.25f),
                        onSecondaryContainer = adjustLightness(secondary, -0.3f),
                        tertiary = adjustLightness(tertiary, -0.1f),
                        onTertiary = adjustLightness(tertiary, -0.4f),
                        tertiaryContainer = adjustLightness(tertiary, 0.25f),
                        onTertiaryContainer = adjustLightness(tertiary, -0.3f),
                        error = adjustLightness(error, -0.1f),
                        onError = adjustLightness(error, -0.4f),
                        errorContainer = adjustLightness(error, 0.25f),
                        onErrorContainer = adjustLightness(error, -0.3f),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = Color(0xFF0E1116),
                        surfaceVariant = adjustLightness(secondary, -0.05f),
                        onSurfaceVariant = adjustLightness(secondary, 0.5f),
                        outline = adjustLightness(secondary, 0.3f),
                        outlineVariant = adjustLightness(secondary, -0.1f),
                        scrim = Color(0xFF000000),
                        inverseSurface = adjustLightness(primary, 0.6f),
                        inverseOnSurface = adjustLightness(primary, -0.5f),
                        inversePrimary = adjustLightness(primary, 0.3f)
                    )
                    ContrastMode.High -> lightColorScheme(
                        primary = adjustLightness(primary, -0.2f),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = adjustLightness(primary, 0.2f),
                        onPrimaryContainer = Color(0xFFFFFFFF),
                        secondary = adjustLightness(secondary, -0.2f),
                        onSecondary = Color(0xFFFFFFFF),
                        secondaryContainer = adjustLightness(secondary, 0.2f),
                        onSecondaryContainer = Color(0xFFFFFFFF),
                        tertiary = adjustLightness(tertiary, -0.2f),
                        onTertiary = Color(0xFFFFFFFF),
                        tertiaryContainer = adjustLightness(tertiary, 0.2f),
                        onTertiaryContainer = Color(0xFFFFFFFF),
                        error = adjustLightness(error, -0.2f),
                        onError = Color(0xFFFFFFFF),
                        errorContainer = adjustLightness(error, 0.2f),
                        onErrorContainer = Color(0xFFFFFFFF),
                        background = background,
                        onBackground = onBackground,
                        surface = surface,
                        onSurface = Color(0xFF000000),
                        surfaceVariant = adjustLightness(secondary, 0.0f),
                        onSurfaceVariant = Color(0xFF000000),
                        outline = adjustLightness(secondary, 0.4f),
                        outlineVariant = adjustLightness(secondary, 0.1f),
                        scrim = Color(0xFF000000),
                        inverseSurface = adjustLightness(primary, 0.7f),
                        inverseOnSurface = Color(0xFF000000),
                        inversePrimary = adjustLightness(primary, 0.2f)
                    )
                }
            }.copy(
                surfaceDim = Color(0xFFD8DAE1),
                surfaceBright = Color(0xFFF9F9FF),
                surfaceContainerLowest = Color(0xFFFFFFFF),
                surfaceContainerLow = Color(0xFFF2F3FB),
                surfaceContainer = Color(0xFFECEDF5),
                surfaceContainerHigh = Color(0xFFE6E8EF),
                surfaceContainerHighest = Color(0xFFE1E2EA)
            )
        }
    }
}