package com.example.animeapp.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
    isPrimary: Boolean = false,
    onItemClick: (() -> Unit)? = null,
    backgroundBrush: Brush? = null,
    outerPadding: PaddingValues = PaddingValues(8.dp),
    innerPadding: PaddingValues = PaddingValues(16.dp)
): Modifier {
    var modifier = this
        .padding(outerPadding)
        .clip(RoundedCornerShape(16.dp))
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(16.dp)
        )
        .then(
            if (backgroundBrush != null) {
                Modifier.background(brush = backgroundBrush)
            } else if (isError) {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.errorContainer
                        )
                    )
                )
            } else if (isPrimary) {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
            } else {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                )
            }
        )

    onItemClick?.let { modifier = modifier.clickable { it() } }

    return modifier.padding(innerPadding)
}

@Composable
fun Modifier.shimmerContainer(): Modifier =
    this
        .padding(8.dp)
        .clip(RoundedCornerShape(16.dp))
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(16.dp)
        )
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f)
                )
            )
        )
        .fillMaxWidth()
        .padding(16.dp)