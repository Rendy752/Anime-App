package com.example.animeapp.ui.animeDetail.episodeDetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.Episode
import com.example.animeapp.models.animeDetailComplementPlaceholder
import com.example.animeapp.models.episodePlaceholder
import com.example.animeapp.ui.animeDetail.DetailAction
import com.example.animeapp.ui.animeDetail.DetailState
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.WatchUtils.getEpisodeBackgroundColor
import com.example.animeapp.utils.basicContainer

@Preview
@Composable
fun EpisodeDetailItem(
    animeDetailComplement: AnimeDetailComplement = animeDetailComplementPlaceholder,
    episode: Episode = episodePlaceholder,
    detailState: DetailState = DetailState(),
    query: String = "",
    onAction: (DetailAction) -> Unit = {},
    onClick: (String) -> Unit = {}
) {
    LaunchedEffect(episode.episodeId) {
        if (detailState.episodeDetailComplements[episode.episodeId] == null) {
            onAction(DetailAction.LoadEpisodeDetailComplement(episode.episodeId))
        }
    }

    val episodeDetailComplementResource = detailState.episodeDetailComplements[episode.episodeId]
    val episodeDetailComplement = (episodeDetailComplementResource as? Resource.Success)?.data

    Row(
        modifier = Modifier
            .basicContainer(
                onItemClick = { onClick(episode.episodeId) },
                backgroundBrush = getEpisodeBackgroundColor(episode.filler, episodeDetailComplement)
            )
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            animeDetailComplement.lastEpisodeWatchedId?.let { lastEpisodeWatchedId ->
                if (episodeDetailComplement?.id == lastEpisodeWatchedId) Text(
                    text = "Last Watched",
                    modifier = Modifier
                        .basicContainer(
                            outerPadding = PaddingValues(2.dp),
                            innerPadding = PaddingValues(8.dp),
                            backgroundBrush = getEpisodeBackgroundColor(
                                episode.filler,
                                isWatching = true
                            )
                        )
                )
            }
            Text(
                text = "Ep. ${episode.episodeNo}",
                modifier = Modifier
                    .basicContainer(
                        outerPadding = PaddingValues(2.dp),
                        innerPadding = PaddingValues(8.dp),
                    )
            )
        }
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

@Preview
@Composable
fun EpisodeDetailItemSkeleton() {
    Row(
        modifier = Modifier
            .basicContainer()
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SkeletonBox(
            width = 60.dp,
            height = 24.dp
        )
        SkeletonBox(
            modifier = Modifier.fillMaxWidth(),
            height = 24.dp
        )
    }
}