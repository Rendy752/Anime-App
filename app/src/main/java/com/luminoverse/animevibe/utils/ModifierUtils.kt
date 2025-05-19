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

@Composable
fun Modifier.basicContainer(
    isError: Boolean = false,
    isTertiary: Boolean = false,
    isPrimary: Boolean = false,
    onItemClick: (() -> Unit)? = null,
    backgroundBrush: Brush? = null,
    roundedCornerShape: RoundedCornerShape = RoundedCornerShape(16.dp),
    outerPadding: PaddingValues = PaddingValues(8.dp),
    innerPadding: PaddingValues = PaddingValues(16.dp),
    alpha: Float = 1.0f
): Modifier {
    var modifier = this
        .padding(outerPadding)
        .clip(roundedCornerShape)
        .border(
            width = 1.dp,
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