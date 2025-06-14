package com.luminoverse.animevibe.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun MessageDisplay(
    modifier: Modifier = Modifier,
    message: String,
    isError: Boolean = true,
    isRounded: Boolean = true
) {
    Text(
        text = message,
        modifier = modifier
            .basicContainer(
                isError = isError,
                isPrimary = !isError,
                roundedCornerShape = if (isRounded) RoundedCornerShape(16.dp) else RoundedCornerShape(
                    0.dp
                ),
                outerPadding = if (isRounded) PaddingValues(8.dp) else PaddingValues(0.dp),
                innerPadding = if (isRounded) PaddingValues(16.dp) else PaddingValues(4.dp)
            ),
        color = MaterialTheme.colorScheme.onError,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}