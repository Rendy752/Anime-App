package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.common.SkeletonBox
import com.luminoverse.animevibe.utils.basicContainer
import com.luminoverse.animevibe.utils.watch.WatchUtils.getEpisodeBackgroundColor

@Composable
fun EpisodeNavigationButton(
    modifier: Modifier = Modifier,
    episodeDetailComplement: EpisodeDetailComplement?,
    episode: Episode?,
    isPrevious: Boolean,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit
) {
    val buttonIcon = if (isPrevious) {
        Icons.AutoMirrored.Filled.ArrowBack
    } else {
        Icons.AutoMirrored.Filled.ArrowForward
    }
    Row(
        modifier = modifier
            .basicContainer(
                backgroundBrush = getEpisodeBackgroundColor(
                    episode?.filler == true,
                    episodeDetailComplement
                ),
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(8.dp),
                onItemClick = {
                    episode?.let { episode ->
                        episodeSourcesQuery?.let { episodeSourcesQuery ->
                            handleSelectedEpisodeServer(
                                episodeSourcesQuery.copy(id = episode.episodeId)
                            )
                        }
                    }
                })
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPrevious) {
            Icon(
                buttonIcon,
                contentDescription = "Previous Episode",
                modifier = Modifier.padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            episode?.name ?: "Unknown",
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Companion.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!isPrevious) {
            Icon(
                buttonIcon,
                contentDescription = "Next Episode",
                modifier = Modifier.padding(start = 8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview
@Composable
fun EpisodeNavigationButtonSkeleton(modifier: Modifier = Modifier, isPrevious: Boolean = false) {
    val buttonIcon = if (isPrevious) {
        Icons.AutoMirrored.Filled.ArrowBack
    } else {
        Icons.AutoMirrored.Filled.ArrowForward
    }
    Row(
        modifier = modifier
            .basicContainer(
                backgroundBrush = getEpisodeBackgroundColor(false),
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(8.dp)
            )
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPrevious) {
            Icon(
                buttonIcon,
                contentDescription = "Previous Episode",
                modifier = Modifier.padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        SkeletonBox(
            width = 100.dp,
            height = 20.dp,
            modifier = Modifier.weight(1f)
        )
        if (!isPrevious) {
            Icon(
                buttonIcon,
                contentDescription = "Next Episode",
                modifier = Modifier.padding(start = 8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}