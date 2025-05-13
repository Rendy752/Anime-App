package com.example.animeapp.ui.episodeHistory.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.common_ui.EpisodeInfoRow
import com.example.animeapp.ui.common_ui.EpisodeInfoRowSkeleton
import com.example.animeapp.ui.common_ui.ImageCardWithContent
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.ui.common_ui.highlightText

@Composable
fun AccordionItem(
    anime: AnimeDetailComplement,
    representativeEpisode: EpisodeDetailComplement?,
    searchQuery: String,
    isExpanded: Boolean,
    onItemClick: () -> Unit,
    onAnimeTitleClick: () -> Unit,
    onAnimeFavoriteToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
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
            IconButton(
                onClick = { onAnimeFavoriteToggle(!anime.isFavorite) },
                modifier = Modifier.semantics {
                    stateDescription =
                        if (anime.isFavorite) "Remove from favorites" else "Add to favorites"
                }
            ) {
                Icon(
                    imageVector = if (anime.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Toggle Anime Favorite",
                    tint = if (anime.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier,
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

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(3) {
                        SkeletonBox(width = 24.dp, height = 24.dp)
                    }
                }
            }
        }
    }
}