package com.example.animeapp.ui.animeWatch.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun AnimeWatchScreen(
    animeId: Int,
    episodeId: String,
    defaultEpisodeId: String
) {
    Text("Anime Watch Screen: Anime ID: $animeId, Episode ID: $episodeId, Default Episode ID: $defaultEpisodeId")
}