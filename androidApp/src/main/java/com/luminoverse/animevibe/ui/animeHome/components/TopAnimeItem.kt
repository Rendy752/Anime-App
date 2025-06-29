package com.luminoverse.animevibe.ui.animeHome.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Score
import androidx.compose.material.icons.filled.Warning
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
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.ui.animeHome.INITIAL_CAROUSEL_HEIGHT
import com.luminoverse.animevibe.ui.common.DataTextWithIcon
import com.luminoverse.animevibe.ui.common.SkeletonBox
import com.luminoverse.animevibe.ui.common.ImageCardWithContent

@Composable
fun TopAnimeItem(animeDetail: AnimeDetail, onItemClick: () -> Unit) {
    ImageCardWithContent(
        imageUrl = animeDetail.images.webp.large_image_url,
        contentDescription = "${animeDetail.title} image cover",
        onItemClick = onItemClick,
        leftContent = {
            Column(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = animeDetail.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DataTextWithIcon(
                        value = animeDetail.score.toString(),
                        icon = Icons.Filled.Score
                    )
                    DataTextWithIcon(
                        value = animeDetail.type ?: "Unknown",
                        icon = Icons.Default.PlayCircle
                    )
                    DataTextWithIcon(
                        value = animeDetail.duration.substringBefore("per").trim(),
                        icon = Icons.Default.AccessTime
                    )
                }
                animeDetail.synopsis?.let { synopsis ->
                    Text(
                        text = synopsis,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        height = INITIAL_CAROUSEL_HEIGHT.dp
    )
}

@Preview
@Composable
fun TopAnimeItemSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(INITIAL_CAROUSEL_HEIGHT.dp)
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.85f)
                .align(Alignment.CenterEnd),
            height = INITIAL_CAROUSEL_HEIGHT.dp
        )

        SkeletonBox(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.85f)
                .align(Alignment.CenterEnd),
            height = INITIAL_CAROUSEL_HEIGHT.dp,
            width = 0.dp
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.6f)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp)
                .align(Alignment.TopStart),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(width = INITIAL_CAROUSEL_HEIGHT.dp * 0.8f, height = 20.dp)
                SkeletonBox(width = INITIAL_CAROUSEL_HEIGHT.dp * 0.9f, height = 20.dp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonBox(width = 40.dp, height = 20.dp)
                SkeletonBox(width = 60.dp, height = 20.dp)
                SkeletonBox(width = 50.dp, height = 20.dp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(width = INITIAL_CAROUSEL_HEIGHT.dp * 0.8f, height = 16.dp)
                SkeletonBox(width = INITIAL_CAROUSEL_HEIGHT.dp * 1f, height = 16.dp)
            }
        }
    }
}

@Preview
@Composable
fun TopAnimeItemError() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(INITIAL_CAROUSEL_HEIGHT.dp)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Error Icon",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Failed to Load Top Anime Info",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Please check your internet connection or try again later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}