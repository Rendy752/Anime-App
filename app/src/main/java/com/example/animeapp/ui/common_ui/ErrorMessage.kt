package com.example.animeapp.ui.common_ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.animeapp.utils.basicContainer

@Composable
fun ErrorMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.basicContainer(isError = true).fillMaxWidth(),
        color = MaterialTheme.colorScheme.onError,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}