package com.luminoverse.animevibe.utils.watch

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
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.ui.theme.defaultEpisode
import com.luminoverse.animevibe.ui.theme.favoriteEpisode
import com.luminoverse.animevibe.ui.theme.fillerEpisode
import com.luminoverse.animevibe.ui.theme.watchedEpisode
import com.luminoverse.animevibe.ui.theme.watchingEpisode

object WatchUtils {
    @Composable
    fun getEpisodeBackgroundColor(
        isFiller: Boolean,
        episodeDetailComplement: EpisodeDetailComplement? = null,
        isWatching: Boolean? = null,
    ): Brush {
        val color = when {
            isWatching == true -> watchingEpisode
            episodeDetailComplement?.isFavorite == true -> favoriteEpisode
            episodeDetailComplement?.lastWatched != null -> watchedEpisode
            isFiller -> fillerEpisode
            else -> defaultEpisode
        }

        return Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                color
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