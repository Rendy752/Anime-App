package com.example.animeapp.ui.common_ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun UnorderedList(
    items: List<String>,
    onItemClick: ((String) -> Unit)? = null,
) {
    Column {
        items.forEach { item ->
            UnorderedListItem(
                text = item,
                onClick = { onItemClick?.invoke(item) },
                textColor = MaterialTheme.colorScheme.primary,
                dotColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}