package com.example.animeapp.ui.animeHome.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.WatchRecentEpisode
import com.example.animeapp.ui.common_ui.AsyncImageWithPlaceholder
import com.example.animeapp.ui.common_ui.ImageRoundedCorner
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.utils.basicContainer

@Composable
fun WatchRecentEpisodeItem(
    episode: WatchRecentEpisode,
    onItemClick: (WatchRecentEpisode) -> Unit
) {
    Column(
        modifier = Modifier
            .basicContainer(
                innerPadding = PaddingValues(0.dp),
                onItemClick = { onItemClick(episode) })
    ) {
        Box {
            AsyncImageWithPlaceholder(
                model = episode.entry.images.jpg.image_url,
                contentDescription = episode.entry.title,
                roundedCorners = ImageRoundedCorner.TOP,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
            )
            if (episode.episodes.isNotEmpty()) {
                Text(
                    modifier = Modifier
                        .basicContainer(
                            innerPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                        )
                        .align(Alignment.TopStart),
                    text = episode.episodes[0].mal_id.toString(),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        Text(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            text = episode.entry.title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
fun WatchRecentEpisodeItemSkeleton() {
    Column(
        modifier = Modifier
            .basicContainer(
                innerPadding = PaddingValues(0.dp)
            )
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        )
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            height = 20.dp
        )
    }
}
