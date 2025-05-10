package com.example.animeapp.utils

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.animeapp.ui.theme.ColorStyle
import com.example.animeapp.ui.theme.ContrastMode

object ColorUtils {
    private data class ColorSet(
        val primary: Color,
        val onPrimary: Color,
        val primaryContainer: Color,
        val onPrimaryContainer: Color,
        val secondary: Color,
        val onSecondary: Color,
        val secondaryContainer: Color,
        val onSecondaryContainer: Color,
        val tertiary: Color,
        val onTertiary: Color,
        val tertiaryContainer: Color,
        val onTertiaryContainer: Color,
        val error: Color,
        val onError: Color,
        val errorContainer: Color,
        val onErrorContainer: Color
    )

    private val defaultColors = mapOf(
        ContrastMode.Normal to mapOf(
            true to ColorSet(
                primary = Color(0xFFA5C8FF), onPrimary = Color(0xFF00315F),
                primaryContainer = Color(0xFF62A4FB), onPrimaryContainer = Color(0xFF00396D),
                secondary = Color(0xFFB1C8EE), onSecondary = Color(0xFF1A3150),
                secondaryContainer = Color(0xFF314868), onSecondaryContainer = Color(0xFFA0B6DC),
                tertiary = Color(0xFFF4AEFF), onTertiary = Color(0xFF530B64),
                tertiaryContainer = Color(0xFFD286E0), onTertiaryContainer = Color(0xFF5C186D),
                error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
                errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6)
            ),
            false to ColorSet(
                primary = Color(0xFF005FAF), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF62A4FB), onPrimaryContainer = Color(0xFF00396D),
                secondary = Color(0xFF495F81), onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFC2D8FF), onSecondaryContainer = Color(0xFF485E80),
                tertiary = Color(0xFF874296), onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFD286E0), onTertiaryContainer = Color(0xFF5C186D),
                error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A)
            )
        ),
        ContrastMode.Medium to mapOf(
            true to ColorSet(
                primary = Color(0xFFCADDFF), onPrimary = Color(0xFF00264C),
                primaryContainer = Color(0xFF62A4FB), onPrimaryContainer = Color(0xFF001732),
                secondary = Color(0xFFCADDFF), onSecondary = Color(0xFF0D2644),
                secondaryContainer = Color(0xFF7C92B6), onSecondaryContainer = Color(0xFF000000),
                tertiary = Color(0xFFFBCEFF), onTertiary = Color(0xFF440055),
                tertiaryContainer = Color(0xFFD286E0), onTertiaryContainer = Color(0xFF2C0038),
                error = Color(0xFFFFD2CC), onError = Color(0xFF540003),
                errorContainer = Color(0xFFFF5449), onErrorContainer = Color(0xFF000000)
            ),
            false to ColorSet(
                primary = Color(0xFF003669), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF1E6EC1), onPrimaryContainer = Color(0xFFFFFFFF),
                secondary = Color(0xFF203756), onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFF586E90), onSecondaryContainer = Color(0xFFFFFFFF),
                tertiary = Color(0xFF59146A), onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFF9751A6), onTertiaryContainer = Color(0xFFFFFFFF),
                error = Color(0xFF740006), onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFCF2C27), onErrorContainer = Color(0xFFFFFFFF)
            )
        ),
        ContrastMode.High to mapOf(
            true to ColorSet(
                primary = Color(0xFFEAF0FF), onPrimary = Color(0xFF000000),
                primaryContainer = Color(0xFF9FC4FF), onPrimaryContainer = Color(0xFF000B1E),
                secondary = Color(0xFFEAF0FF), onSecondary = Color(0xFF000000),
                secondaryContainer = Color(0xFFADC4EA), onSecondaryContainer = Color(0xFF000B1E),
                tertiary = Color(0xFFFFEAFD), onTertiary = Color(0xFF000000),
                tertiaryContainer = Color(0xFFF3A8FF), onTertiaryContainer = Color(0xFF1A0022),
                error = Color(0xFFFFECE9), onError = Color(0xFF000000),
                errorContainer = Color(0xFFFFAEA4), onErrorContainer = Color(0xFF220001)
            ),
            false to ColorSet(
                primary = Color(0xFF002C57), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF004A8A), onPrimaryContainer = Color(0xFFFFFFFF),
                secondary = Color(0xFF152D4B), onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFF344A6A), onSecondaryContainer = Color(0xFFFFFFFF),
                tertiary = Color(0xFF4E045F), onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFF6F2B7F), onTertiaryContainer = Color(0xFFFFFFFF),
                error = Color(0xFF600004), onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFF98000A), onErrorContainer = Color(0xFFFFFFFF)
            )
        )
    )

    private val vibrantColors = mapOf(
        ContrastMode.Normal to mapOf(
            true to ColorSet(
                primary = Color(0xFF80FFFF), onPrimary = Color(0xFF003737),
                primaryContainer = Color(0xFF006C6C), onPrimaryContainer = Color(0xFF80FFFF),
                secondary = Color(0xFFFFA180), onSecondary = Color(0xFF4B0000),
                secondaryContainer = Color(0xFFBF2A2A), onSecondaryContainer = Color(0xFFFFA180),
                tertiary = Color(0xFFC1E8A8), onTertiary = Color(0xFF1A3C19),
                tertiaryContainer = Color(0xFF4B8A4A), onTertiaryContainer = Color(0xFFC1E8A8),
                error = Color(0xFFFF99CC), onError = Color(0xFF5C0030),
                errorContainer = Color(0xFFBF0066), onErrorContainer = Color(0xFFFF99CC)
            ),
            false to ColorSet(
                primary = Color(0xFF00B7B7), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF4DE3E3), onPrimaryContainer = Color(0xFF002A2A),
                secondary = Color(0xFFFF4D4D), onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFFF8A8A), onSecondaryContainer = Color(0xFF4B0000),
                tertiary = Color(0xFF76D275), onTertiary = Color(0xFF002100),
                tertiaryContainer = Color(0xFF9EF99E), onTertiaryContainer = Color(0xFF002100),
                error = Color(0xFFFF2A8D), onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFF80BF), onErrorContainer = Color(0xFF3C0022)
            )
        ),
        ContrastMode.Medium to mapOf(
            true to ColorSet(
                primary = Color(0xFFA8FFFF), onPrimary = Color(0xFF002323),
                primaryContainer = Color(0xFF006C6C), onPrimaryContainer = Color(0xFF001A1A),
                secondary = Color(0xFFFFC2A1), onSecondary = Color(0xFF330000),
                secondaryContainer = Color(0xFFE04A4A), onSecondaryContainer = Color(0xFF1A0000),
                tertiary = Color(0xFFD4F4B8), onTertiary = Color(0xFF0F2A0F),
                tertiaryContainer = Color(0xFF4B8A4A), onTertiaryContainer = Color(0xFF0A1F0A),
                error = Color(0xFFFFB3D6), onError = Color(0xFF3F0020),
                errorContainer = Color(0xFFE6007E), onErrorContainer = Color(0xFF260013)
            ),
            false to ColorSet(
                primary = Color(0xFF008A8A), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF33C7C7), onPrimaryContainer = Color(0xFF001F1F),
                secondary = Color(0xFFCC3F3F), onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFE86E6E), onSecondaryContainer = Color(0xFF2A0000),
                tertiary = Color(0xFF5AA559), onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFF8CCB8B), onTertiaryContainer = Color(0xFF002900),
                error = Color(0xFFCC226F), onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFE667A3), onErrorContainer = Color(0xFF2A0016)
            )
        ),
        ContrastMode.High to mapOf(
            true to ColorSet(
                primary = Color(0xFFD1FFFF), onPrimary = Color(0xFF000000),
                primaryContainer = Color(0xFF4DE3E3), onPrimaryContainer = Color(0xFF000000),
                secondary = Color(0xFFFFE0CC), onSecondary = Color(0xFF000000),
                secondaryContainer = Color(0xFFFF8A8A), onSecondaryContainer = Color(0xFF000000),
                tertiary = Color(0xFFE8FFD8), onTertiary = Color(0xFF000000),
                tertiaryContainer = Color(0xFF9EF99E), onTertiaryContainer = Color(0xFF000000),
                error = Color(0xFFFFD6E8), onError = Color(0xFF000000),
                errorContainer = Color(0xFFFF80BF), onErrorContainer = Color(0xFF000000)
            ),
            false to ColorSet(
                primary = Color(0xFF006C6C), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF00A3A3), onPrimaryContainer = Color(0xFFFFFFFF),
                secondary = Color(0xFFAA3333), onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFD15555), onSecondaryContainer = Color(0xFFFFFFFF),
                tertiary = Color(0xFF4A8749), onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFF6FA86E), onTertiaryContainer = Color(0xFFFFFFFF),
                error = Color(0xFFAA1C5C), onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFD14D8A), onErrorContainer = Color(0xFFFFFFFF)
            )
        )
    )

    private val monochromeColors = mapOf(
        ContrastMode.Normal to mapOf(
            true to ColorSet(
                primary = Color(0xFFD1D1D1), onPrimary = Color(0xFF2E2E2E),
                primaryContainer = Color(0xFF666666), onPrimaryContainer = Color(0xFFD1D1D1),
                secondary = Color(0xFFE6E6E6), onSecondary = Color(0xFF1C1C1C),
                secondaryContainer = Color(0xFF4D4D4D), onSecondaryContainer = Color(0xFFE6E6E6),
                tertiary = Color(0xFFF0F0F0), onTertiary = Color(0xFF3C3C3C),
                tertiaryContainer = Color(0xFF7A7A7A), onTertiaryContainer = Color(0xFFF0F0F0),
                error = Color(0xFFDADADA), onError = Color(0xFF2A2A2A),
                errorContainer = Color(0xFF595959), onErrorContainer = Color(0xFFDADADA)
            ),
            false to ColorSet(
                primary = Color(0xFF8C8C8C), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFB8B8B8), onPrimaryContainer = Color(0xFF2E2E2E),
                secondary = Color(0xFF737373), onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFF9E9E9E), onSecondaryContainer = Color(0xFF1C1C1C),
                tertiary = Color(0xFFA3A3A3), onTertiary = Color(0xFF000000),
                tertiaryContainer = Color(0xFFCCCCCC), onTertiaryContainer = Color(0xFF000000),
                error = Color(0xFF7A7A7A), onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFA6A6A6), onErrorContainer = Color(0xFF2A2A2A)
            )
        ),
        ContrastMode.Medium to mapOf(
            true to ColorSet(
                primary = Color(0xFFE0E0E0), onPrimary = Color(0xFF1A1A1A),
                primaryContainer = Color(0xFF666666), onPrimaryContainer = Color(0xFF0F0F0F),
                secondary = Color(0xFFF0F0F0), onSecondary = Color(0xFF0F0F0F),
                secondaryContainer = Color(0xFFB8B8B8), onSecondaryContainer = Color(0xFF000000),
                tertiary = Color(0xFFF5F5F5), onTertiary = Color(0xFF262626),
                tertiaryContainer = Color(0xFF7A7A7A), onTertiaryContainer = Color(0xFF1A1A1A),
                error = Color(0xFFE8E8E8), onError = Color(0xFF1F1F1F),
                errorContainer = Color(0xFFB0B0B0), onErrorContainer = Color(0xFF000000)
            ),
            false to ColorSet(
                primary = Color(0xFF737373), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFA3A3A3), onPrimaryContainer = Color(0xFF1A1A1A),
                secondary = Color(0xFF5C5C5C), onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFF8C8C8C), onSecondaryContainer = Color(0xFF0F0F0F),
                tertiary = Color(0xFF8C8C8C), onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFB8B8B8), onTertiaryContainer = Color(0xFF262626),
                error = Color(0xFF666666), onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFF999999), onErrorContainer = Color(0xFF1F1F1F)
            )
        ),
        ContrastMode.High to mapOf(
            true to ColorSet(
                primary = Color(0xFFF0F0F0), onPrimary = Color(0xFF000000),
                primaryContainer = Color(0xFFB8B8B8), onPrimaryContainer = Color(0xFF000000),
                secondary = Color(0xFFF5F5F5), onSecondary = Color(0xFF000000),
                secondaryContainer = Color(0xFFCCCCCC), onSecondaryContainer = Color(0xFF000000),
                tertiary = Color(0xFFFAFAFA), onTertiary = Color(0xFF000000),
                tertiaryContainer = Color(0xFFDADADA), onTertiaryContainer = Color(0xFF000000),
                error = Color(0xFFEDEDED), onError = Color(0xFF000000),
                errorContainer = Color(0xFFC6C6C6), onErrorContainer = Color(0xFF000000)
            ),
            false to ColorSet(
                primary = Color(0xFF595959), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF8C8C8C), onPrimaryContainer = Color(0xFFFFFFFF),
                secondary = Color(0xFF4D4D4D), onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFF737373), onSecondaryContainer = Color(0xFFFFFFFF),
                tertiary = Color(0xFF737373), onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFF9E9E9E), onTertiaryContainer = Color(0xFFFFFFFF),
                error = Color(0xFF595959), onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFF8C8C8C), onErrorContainer = Color(0xFFFFFFFF)
            )
        )
    )

    private data class SurfaceColors(
        val dim: Color,
        val bright: Color,
        val containerLowest: Color,
        val containerLow: Color,
        val container: Color,
        val containerHigh: Color,
        val containerHighest: Color
    )

    private val surfaceColors = mapOf(
        true to SurfaceColors(
            dim = Color(0xFF101319), bright = Color(0xFF36393F),
            containerLowest = Color(0xFF0B0E13), containerLow = Color(0xFF191C21),
            container = Color(0xFF1D2025), containerHigh = Color(0xFF272A30),
            containerHighest = Color(0xFF32353B)
        ),
        false to SurfaceColors(
            dim = Color(0xFFD8DAE1), bright = Color(0xFFF9F9FF),
            containerLowest = Color(0xFFFFFFFF), containerLow = Color(0xFFF2F3FB),
            container = Color(0xFFECEDF5), containerHigh = Color(0xFFE6E8EF),
            containerHighest = Color(0xFFE1E2EA)
        )
    )

    private data class CommonColors(
        val background: Color,
        val onBackground: Color,
        val surface: Color,
        val onSurface: Color,
        val surfaceVariant: Color,
        val onSurfaceVariant: Color,
        val outline: Color,
        val outlineVariant: Color,
        val scrim: Color,
        val inverseSurface: Color,
        val inverseOnSurface: Color,
        val inversePrimary: Color
    )

    private fun getCommonColors(
        isDark: Boolean,
        style: ColorStyle,
        contrast: ContrastMode
    ): CommonColors {
        val (background, onBackground, surface, onSurface) = if (isDark) {
            listOf(Color(0xFF101319), Color(0xFFE1E2EA), Color(0xFF101319), Color(0xFFE1E2EA))
        } else {
            listOf(Color(0xFFF9F9FF), Color(0xFF191C21), Color(0xFFF9F9FF), Color(0xFF191C21))
        }

        return when (style) {
            ColorStyle.Default -> CommonColors(
                background, onBackground, surface,
                onSurface = if (isDark && contrast != ContrastMode.Normal) Color.White else onSurface,
                surfaceVariant = if (isDark) Color(0xFF414752) else Color(0xFFDDE2F0),
                onSurfaceVariant = when {
                    isDark && contrast == ContrastMode.High -> Color.White
                    isDark && contrast == ContrastMode.Medium -> Color(0xFFD7DCE9)
                    isDark -> Color(0xFFC1C6D3)
                    contrast == ContrastMode.High -> Color.Black
                    contrast == ContrastMode.Medium -> Color(0xFF313641)
                    else -> Color(0xFF414752)
                },
                outline = when {
                    isDark && contrast == ContrastMode.High -> Color(0xFFEBF0FD)
                    isDark && contrast == ContrastMode.Medium -> Color(0xFFACB2BF)
                    isDark -> Color(0xFF8B919D)
                    contrast == ContrastMode.High -> Color(0xFF272C36)
                    contrast == ContrastMode.Medium -> Color(0xFF4D535D)
                    else -> Color(0xFF727783)
                },
                outlineVariant = when {
                    isDark && contrast == ContrastMode.High -> Color(0xFFBDC3CF)
                    isDark && contrast == ContrastMode.Medium -> Color(0xFF8B909C)
                    isDark -> Color(0xFF414752)
                    contrast == ContrastMode.High -> Color(0xFF444954)
                    contrast == ContrastMode.Medium -> Color(0xFF686D79)
                    else -> Color(0xFFC1C6D3)
                },
                scrim = Color.Black,
                inverseSurface = if (isDark) Color(0xFFE1E2EA) else Color(0xFF2E3036),
                inverseOnSurface = when {
                    isDark && contrast == ContrastMode.High -> Color.Black
                    isDark && contrast == ContrastMode.Medium -> Color(0xFF272A30)
                    isDark -> Color(0xFF2E3036)
                    contrast == ContrastMode.High -> Color.White
                    else -> Color(0xFFEFF0F8)
                },
                inversePrimary = if (isDark) Color(0xFF005FAF) else Color(0xFFA5C8FF)
            )

            ColorStyle.Vibrant -> CommonColors(
                background, onBackground, surface,
                onSurface = if (isDark && contrast != ContrastMode.Normal) Color.White else onSurface,
                surfaceVariant = if (isDark) Color(0xFF3A4A4A) else Color(0xFFD1E2E2),
                onSurfaceVariant = when {
                    isDark && contrast == ContrastMode.High -> Color.White
                    isDark && contrast == ContrastMode.Medium -> Color(0xFFC6D7D7)
                    isDark -> Color(0xFFB0C6C6)
                    contrast == ContrastMode.High -> Color.Black
                    contrast == ContrastMode.Medium -> Color(0xFF2E3A3A)
                    else -> Color(0xFF3A4A4A)
                },
                outline = when {
                    isDark && contrast == ContrastMode.High -> Color(0xFFB3C6C6)
                    isDark && contrast == ContrastMode.Medium -> Color(0xFF99B3B3)
                    isDark -> Color(0xFF80A3A3)
                    contrast == ContrastMode.High -> Color(0xFF4A5C5C)
                    contrast == ContrastMode.Medium -> Color(0xFF5C7373)
                    else -> Color(0xFF6B7A7A)
                },
                outlineVariant = when {
                    isDark && contrast == ContrastMode.High -> Color(0xFF809999)
                    isDark && contrast == ContrastMode.Medium -> Color(0xFF5C7373)
                    isDark -> Color(0xFF3A4A4A)
                    contrast == ContrastMode.High -> Color(0xFF668080)
                    contrast == ContrastMode.Medium -> Color(0xFF809999)
                    else -> Color(0xFFB0C6C6)
                },
                scrim = Color.Black,
                inverseSurface = if (isDark) Color(0xFFE1E2EA) else Color(0xFF2E3036),
                inverseOnSurface = when {
                    isDark && contrast == ContrastMode.High -> Color.Black
                    isDark && contrast == ContrastMode.Medium -> Color(0xFF272A30)
                    isDark -> Color(0xFF2E3036)
                    contrast == ContrastMode.High -> Color.White
                    else -> Color(0xFFEFF0F8)
                },
                inversePrimary = if (isDark) Color(0xFF00B7B7) else Color(0xFF80FFFF)
            )

            ColorStyle.Monochrome -> CommonColors(
                background, onBackground, surface,
                onSurface = if (isDark && contrast != ContrastMode.Normal) Color.White else onSurface,
                surfaceVariant = if (isDark) Color(0xFF414752) else Color(0xFFDDE2F0),
                onSurfaceVariant = when {
                    isDark && contrast == ContrastMode.High -> Color.White
                    isDark && contrast == ContrastMode.Medium -> Color(0xFFD7DCE9)
                    isDark -> Color(0xFFC1C6D3)
                    contrast == ContrastMode.High -> Color.Black
                    contrast == ContrastMode.Medium -> Color(0xFF313641)
                    else -> Color(0xFF414752)
                },
                outline = when {
                    isDark && contrast == ContrastMode.High -> Color(0xFFB8B8B8)
                    isDark && contrast == ContrastMode.Medium -> Color(0xFFA3A3A3)
                    isDark -> Color(0xFF8B919D)
                    contrast == ContrastMode.High -> Color(0xFF4D4D4D)
                    contrast == ContrastMode.Medium -> Color(0xFF5C5C5C)
                    else -> Color(0xFF727783)
                },
                outlineVariant = when {
                    isDark && contrast == ContrastMode.High -> Color(0xFF8C8C8C)
                    isDark && contrast == ContrastMode.Medium -> Color(0xFF737373)
                    isDark -> Color(0xFF414752)
                    contrast == ContrastMode.High -> Color(0xFF737373)
                    contrast == ContrastMode.Medium -> Color(0xFF8C8C8C)
                    else -> Color(0xFFC1C6D3)
                },
                scrim = Color.Black,
                inverseSurface = if (isDark) Color(0xFFE1E2EA) else Color(0xFF2E3036),
                inverseOnSurface = when {
                    isDark && contrast == ContrastMode.High -> Color.Black
                    isDark && contrast == ContrastMode.Medium -> Color(0xFF272A30)
                    isDark -> Color(0xFF2E3036)
                    contrast == ContrastMode.High -> Color.White
                    else -> Color(0xFFEFF0F8)
                },
                inversePrimary = if (isDark) Color(0xFF8C8C8C) else Color(0xFFD1D1D1)
            )
        }
    }

    fun generateColorScheme(
        style: ColorStyle,
        isDark: Boolean,
        contrast: ContrastMode
    ): ColorScheme {
        val colorSet = when (style) {
            ColorStyle.Default -> defaultColors[contrast]?.get(isDark)
            ColorStyle.Vibrant -> vibrantColors[contrast]?.get(isDark)
            ColorStyle.Monochrome -> monochromeColors[contrast]?.get(isDark)
        }
            ?: throw IllegalStateException("Invalid color configuration for $style, $contrast, isDark=$isDark")

        val common = getCommonColors(isDark, style, contrast)
        val scheme = if (isDark) {
            darkColorScheme(
                primary = colorSet.primary,
                onPrimary = colorSet.onPrimary,
                primaryContainer = colorSet.primaryContainer,
                onPrimaryContainer = colorSet.onPrimaryContainer,
                secondary = colorSet.secondary,
                onSecondary = colorSet.onSecondary,
                secondaryContainer = colorSet.secondaryContainer,
                onSecondaryContainer = colorSet.onSecondaryContainer,
                tertiary = colorSet.tertiary,
                onTertiary = colorSet.onTertiary,
                tertiaryContainer = colorSet.tertiaryContainer,
                onTertiaryContainer = colorSet.onTertiaryContainer,
                error = colorSet.error,
                onError = colorSet.onError,
                errorContainer = colorSet.errorContainer,
                onErrorContainer = colorSet.onErrorContainer,
                background = common.background,
                onBackground = common.onBackground,
                surface = common.surface,
                onSurface = common.onSurface,
                surfaceVariant = common.surfaceVariant,
                onSurfaceVariant = common.onSurfaceVariant,
                outline = common.outline,
                outlineVariant = common.outlineVariant,
                scrim = common.scrim,
                inverseSurface = common.inverseSurface,
                inverseOnSurface = common.inverseOnSurface,
                inversePrimary = common.inversePrimary
            )
        } else {
            lightColorScheme(
                primary = colorSet.primary,
                onPrimary = colorSet.onPrimary,
                primaryContainer = colorSet.primaryContainer,
                onPrimaryContainer = colorSet.onPrimaryContainer,
                secondary = colorSet.secondary,
                onSecondary = colorSet.onSecondary,
                secondaryContainer = colorSet.secondaryContainer,
                onSecondaryContainer = colorSet.onSecondaryContainer,
                tertiary = colorSet.tertiary,
                onTertiary = colorSet.onTertiary,
                tertiaryContainer = colorSet.tertiaryContainer,
                onTertiaryContainer = colorSet.onTertiaryContainer,
                error = colorSet.error,
                onError = colorSet.onError,
                errorContainer = colorSet.errorContainer,
                onErrorContainer = colorSet.onErrorContainer,
                background = common.background,
                onBackground = common.onBackground,
                surface = common.surface,
                onSurface = common.onSurface,
                surfaceVariant = common.surfaceVariant,
                onSurfaceVariant = common.onSurfaceVariant,
                outline = common.outline,
                outlineVariant = common.outlineVariant,
                scrim = common.scrim,
                inverseSurface = common.inverseSurface,
                inverseOnSurface = common.inverseOnSurface,
                inversePrimary = common.inversePrimary
            )
        }

        val surface = surfaceColors[isDark]
            ?: throw IllegalStateException("Invalid surface colors for isDark=$isDark")
        return scheme.copy(
            surfaceDim = surface.dim,
            surfaceBright = surface.bright,
            surfaceContainerLowest = surface.containerLowest,
            surfaceContainerLow = surface.containerLow,
            surfaceContainer = surface.container,
            surfaceContainerHigh = surface.containerHigh,
            surfaceContainerHighest = surface.containerHighest
        )
    }
}