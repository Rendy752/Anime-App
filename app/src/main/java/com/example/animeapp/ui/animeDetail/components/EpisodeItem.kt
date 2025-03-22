package com.example.animeapp.ui.animeDetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.Episode
import com.example.animeapp.ui.theme.defaultEpisode
import com.example.animeapp.ui.theme.fillerEpisode
import com.example.animeapp.utils.basicContainer

@Composable
fun EpisodeItem(episode: Episode, query: String, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .basicContainer(
                onItemClick = { onClick(episode.episodeId) },
                backgroundBrush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        if (episode.filler) fillerEpisode else defaultEpisode
                    )
                )
            )
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Ep. ${episode.episodeNo}",
            modifier = Modifier.basicContainer()
        )
        Text(
            text = highlightText(episode.name, query)
        )
    }
}

@Composable
fun highlightText(text: String, query: String): AnnotatedString {
    val annotatedString = AnnotatedString.Builder(text)
    if (query.isNotBlank()) {
        val startIndex = text.indexOf(query, ignoreCase = true)
        if (startIndex != -1) {
            annotatedString.addStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                ),
                start = startIndex,
                end = startIndex + query.length
            )
        }
    }
    return annotatedString.toAnnotatedString()
}