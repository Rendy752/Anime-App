package com.luminoverse.animevibe.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun PaginationButton(
    text: String,
    pageNumber: Int,
    currentPage: Int,
    onPaginationClick: (Int) -> Unit
) {
    val isCurrentPage = currentPage == pageNumber
    val backgroundColor = if (isCurrentPage) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isCurrentPage) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = text,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable {
                if (!isCurrentPage) {
                    onPaginationClick(pageNumber)
                }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = textColor,
        style = MaterialTheme.typography.bodyMedium
    )
}