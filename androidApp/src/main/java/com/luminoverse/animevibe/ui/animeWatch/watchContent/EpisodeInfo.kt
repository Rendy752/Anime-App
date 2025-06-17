package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.common.SkeletonBox
import com.luminoverse.animevibe.utils.watch.WatchUtils.getServerCategoryIcon

@Composable
fun EpisodeInfo(
    title: String?,
    episode: Episode,
    episodeNo: Int,
    episodeSourcesQuery: EpisodeSourcesQuery?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center
        ) {
            if (title != null) Text(
                text = if (episode.title != "Full") episode.title else title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ) else {
                SkeletonBox(
                    width = 100.dp,
                    height = 20.dp
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Eps. $episodeNo",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            episodeSourcesQuery?.let { query ->
                Row(modifier = Modifier.padding(start = 4.dp)) {
                    getServerCategoryIcon(query.category)?.invoke()
                }
            }
        }
    }
}

@Preview
@Composable
fun EpisodeInfoSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center
        ) {
            SkeletonBox(width = 100.dp, height = 20.dp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBox(width = 60.dp, height = 16.dp)
            SkeletonBox(width = 24.dp, height = 24.dp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}