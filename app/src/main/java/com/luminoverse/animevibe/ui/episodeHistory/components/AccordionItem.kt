package com.luminoverse.animevibe.ui.episodeHistory.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.AnimeDetailComplement
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.ui.common.DebouncedIconButton
import com.luminoverse.animevibe.ui.common.EpisodeInfoRow
import com.luminoverse.animevibe.ui.common.EpisodeInfoRowSkeleton
import com.luminoverse.animevibe.ui.common.ImageCardWithContent
import com.luminoverse.animevibe.ui.common.SkeletonBox
import com.luminoverse.animevibe.ui.common.highlightText

@Composable
fun AccordionItem(
    anime: AnimeDetailComplement,
    representativeEpisode: EpisodeDetailComplement?,
    searchQuery: String,
    isExpanded: Boolean,
    onItemClick: () -> Unit,
    onAnimeTitleClick: () -> Unit,
    onAnimeFavoriteToggle: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    ImageCardWithContent(
        imageUrl = representativeEpisode?.imageUrl,
        contentBackgroundColor = MaterialTheme.colorScheme.surfaceContainer,
        contentDescription = representativeEpisode?.animeTitle ?: "Anime Image",
        onItemClick = onItemClick,
        leftContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    modifier = Modifier.clickable(onClick = onAnimeTitleClick),
                    text = highlightText(
                        representativeEpisode?.animeTitle ?: "Anime ID: ${anime.malId}",
                        searchQuery
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (anime.episodes?.isNotEmpty() == true) {
                        EpisodeInfoRow(
                            subCount = anime.sub,
                            dubCount = anime.dub,
                            epsCount = anime.eps
                        )
                    }
                }
            }
        },
        rightContent = {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.2f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DebouncedIconButton(onClick = { onAnimeFavoriteToggle(!anime.isFavorite) }) {
                    Icon(
                        imageVector = if (anime.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Toggle Anime Favorite",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Anime",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                IconButton(onClick = onItemClick) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        height = 160.dp
    )
}

@Preview
@Composable
fun AccordionItemSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.75f)
                .align(Alignment.CenterEnd),
            height = 160.dp
        )

        SkeletonBox(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.75f)
                .align(Alignment.CenterEnd),
            height = 160.dp,
            width = 0.dp
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.6f)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Column {
                    SkeletonBox(width = 160.dp * 0.8f, height = 20.dp)
                    SkeletonBox(width = 160.dp * 1f, height = 20.dp)
                    SkeletonBox(width = 160.dp * 0.9f, height = 20.dp)
                }

                EpisodeInfoRowSkeleton()
            }

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.2f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DebouncedIconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.FavoriteBorder,
                        contentDescription = "Toggle Anime Favorite",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }

                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Anime",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.ExpandLess,
                        contentDescription = "Collapse",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}