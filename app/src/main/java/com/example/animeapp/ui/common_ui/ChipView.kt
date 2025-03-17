package com.example.animeapp.ui.common_ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

@Composable
fun ChipView(
    text: String,
    onClick: (() -> Unit)? = null,
) {
    var isClicked by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val borderStroke = BorderStroke(1.dp, borderColor)
    val textPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

    val modifier = if (onClick != null) {
        Modifier.clickable {
            onClick()
        }
    } else Modifier

    Surface(
        modifier = modifier,
        onClick = {
            if (onClick != null) {
                isClicked = !isClicked
            }
        },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = borderStroke
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(textPadding),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}