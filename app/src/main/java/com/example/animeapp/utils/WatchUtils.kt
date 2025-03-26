package com.example.animeapp.utils

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.theme.defaultEpisode
import com.example.animeapp.ui.theme.favoriteEpisode
import com.example.animeapp.ui.theme.fillerEpisode
import com.example.animeapp.ui.theme.watchedEpisode
import com.example.animeapp.ui.theme.watchingEpisode

object WatchUtils {
    @Composable
    fun getEpisodeBackgroundColor(
        isFiller: Boolean,
        episodeDetailComplement: EpisodeDetailComplement? = null,
        isWatching: Boolean? = null,
    ): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                if (isWatching == true) watchingEpisode
                else if (episodeDetailComplement?.isFavorite == true) favoriteEpisode
                else if (episodeDetailComplement?.isWatched == true) watchedEpisode
                else if (isFiller) fillerEpisode
                else defaultEpisode
            )
        )
    }

    @Composable
    fun getServerCategoryIcon(category: String): @Composable (() -> Unit)? {
        return when (category.lowercase()) {
            "sub" -> {
                {
                    Icon(
                        imageVector = Icons.Filled.ClosedCaption,
                        contentDescription = "Subtitles",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            "dub" -> {
                {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Dubbed",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            "raw" -> {
                {
                    Icon(
                        imageVector = Icons.Filled.LiveTv,
                        contentDescription = "Raw",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            else -> null
        }
    }
}