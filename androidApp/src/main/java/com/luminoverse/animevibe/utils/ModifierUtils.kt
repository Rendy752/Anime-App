package com.luminoverse.animevibe.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Applies a basic container styling to a Composable.
 *
 * This modifier provides a consistent look and feel for container elements,
 * offering options for background color, border, padding, rounded corners,
 * and click handling.
 *
 * @param isError If true, the background will use error colors.
 * @param isTertiary If true, the background will use tertiary colors.
 * @param isPrimary If true, the background will use primary colors.
 * @param useBorder If true, a border will be applied.
 * @param onItemClick A lambda to be executed when the container is clicked.
 * @param backgroundBrush A custom [Brush] to use for the background. Overrides color-based backgrounds.
 * @param roundedCornerShape The [RoundedCornerShape] to apply to the container.
 * @param outerPadding The padding to apply outside the border.
 * @param innerPadding The padding to apply inside the border (content padding).
 * @param alpha The alpha transparency to apply to the background.
 * @return A [Modifier] with the specified container styling applied.
 */
@Composable
fun Modifier.basicContainer(
    isError: Boolean = false,
    isTertiary: Boolean = false,
    isPrimary: Boolean = false,
    useBorder: Boolean = true,
    onItemClick: (() -> Unit)? = null,
    backgroundBrush: Brush? = null,
    roundedCornerShape: RoundedCornerShape = RoundedCornerShape(16.dp),
    outerPadding: PaddingValues = PaddingValues(8.dp),
    innerPadding: PaddingValues = PaddingValues(16.dp),
    alpha: Float = 1.0f,
): Modifier {
    var modifier = this
        .padding(outerPadding)
        .clip(roundedCornerShape)
        .border(
            width = if (useBorder) 1.dp else 0.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = roundedCornerShape
        )
        .then(
            if (backgroundBrush != null) {
                Modifier.background(brush = backgroundBrush, alpha = alpha)
            } else if (isError) {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.errorContainer
                        ),
                    ),
                    alpha = alpha
                )
            } else if (isTertiary) {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.tertiaryContainer
                        ),
                    ),
                    alpha = alpha
                )
            } else if (isPrimary) {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        ),
                    ),
                    alpha = alpha
                )
            } else {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                    ),
                    alpha = alpha
                )
            }
        )

    onItemClick?.let { modifier = modifier.clickable { it() } }

    return modifier.padding(innerPadding)
}