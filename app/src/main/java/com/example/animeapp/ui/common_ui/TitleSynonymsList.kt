package com.example.animeapp.ui.common_ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TitleSynonymsList(
    synonyms: List<String>,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        synonyms.forEach { synonym ->
            Text(
                text = synonym,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}