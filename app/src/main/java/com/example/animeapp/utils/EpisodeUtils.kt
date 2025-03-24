package com.example.animeapp.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.theme.defaultEpisode
import com.example.animeapp.ui.theme.favoriteEpisode
import com.example.animeapp.ui.theme.fillerEpisode
import com.example.animeapp.ui.theme.watchedEpisode

object EpisodeUtils {
    @Composable
    fun getEpisodeBackgroundColor(
        isFiller: Boolean,
        episodeDetailComplement: EpisodeDetailComplement? = null
    ): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                if (episodeDetailComplement?.isFavorite == true) favoriteEpisode
                else if (episodeDetailComplement?.isWatched == true) watchedEpisode
                else if (isFiller) fillerEpisode
                else defaultEpisode
            )
        )
    }
}