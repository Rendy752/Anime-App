package com.example.animeapp.ui.common_ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable

@Composable
fun TitleSynonymsList(
    synonyms: List<String>,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        synonyms.forEach { synonym -> FilterChipView(synonym) }
    }
}