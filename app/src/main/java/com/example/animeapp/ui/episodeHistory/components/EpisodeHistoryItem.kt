package com.example.animeapp.ui.episodeHistory.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.common_ui.ConfirmationAlert
import com.example.animeapp.ui.common_ui.ScreenshotDisplay
import com.example.animeapp.ui.common_ui.highlightText
import com.example.animeapp.ui.common_ui.DebouncedIconButton
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.ui.theme.favoriteEpisode
import com.example.animeapp.utils.TimeUtils
import com.example.animeapp.utils.WatchUtils.getEpisodeBackgroundColor
import com.example.animeapp.utils.basicContainer
import kotlin.math.roundToInt

@Composable
fun EpisodeHistoryItem(
    searchQuery: String,
    isFirstItem: Boolean,
    episode: EpisodeDetailComplement,
    onClick: () -> Unit,
    onFavoriteToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        ConfirmationAlert(
            title = "Delete Episode",
            message = "Are you sure you want to delete Episode History ${episode.number}: ${episode.episodeTitle}?",
            confirmText = "Delete",
            onConfirm = { onDelete() },
            cancelText = "Cancel",
            onCancel = { showDeleteDialog = false }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .basicContainer(
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(8.dp),
                roundedCornerShape = RoundedCornerShape(
                    topStart = if (isFirstItem) 0.dp else 16.dp,
                    topEnd = if (isFirstItem) 0.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                backgroundBrush = getEpisodeBackgroundColor(episode.isFiller),
                onItemClick = { onClick() }
            )
            .clip(RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScreenshotDisplay(
            modifier = Modifier
                .size(100.dp, 56.dp)
                .clip(RoundedCornerShape(4.dp)),
            imageUrl = episode.imageUrl,
            screenshot = episode.screenshot
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Ep ${episode.number}: ")
                    append(highlightText(episode.episodeTitle, searchQuery))
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                episode.lastTimestamp?.let { timestamp ->
                    Text(
                        modifier = Modifier.padding(end = if (episode.lastWatched != null) 4.dp else 0.dp),
                        text = "${TimeUtils.formatTimestamp(timestamp)} " +
                                "${episode.duration?.let { "/ ${TimeUtils.formatTimestamp(it)}" }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                episode.lastWatched?.let { lastWatched ->
                    Text(
                        modifier = Modifier.padding(start = if (episode.lastTimestamp != null) 4.dp else 0.dp),
                        text = "~ ${TimeUtils.formatDateToAgo(lastWatched)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            val duration = episode.duration?.toFloat() ?: (24 * 60f)
            val progress = episode.lastTimestamp?.let { timestamp ->
                if (timestamp < duration) (timestamp.toFloat() / duration).coerceIn(0f, 1f) else 1f
            } ?: 0f
            val percentage = (progress * 100).roundToInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .semantics { contentDescription = "$percentage% watched" },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DebouncedIconButton(
                onClick = { onFavoriteToggle(!episode.isFavorite) },
                modifier = Modifier.semantics {
                    contentDescription =
                        if (episode.isFavorite) "Remove episode from favorites" else "Add episode to favorites"
                }
            ) {
                Icon(
                    imageVector = if (episode.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = favoriteEpisode
                )
            }
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.semantics { contentDescription = "Delete Episode" }
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview
@Composable
fun EpisodeHistoryItemSkeleton(
    modifier: Modifier = Modifier,
    isFirstItem: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .basicContainer(
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(8.dp),
                roundedCornerShape = RoundedCornerShape(
                    topStart = if (isFirstItem) 0.dp else 16.dp,
                    topEnd = if (isFirstItem) 0.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                backgroundBrush = null,
                onItemClick = {}
            )
            .clip(RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBox(
            modifier = Modifier
                .size(100.dp, 56.dp)
                .clip(RoundedCornerShape(4.dp)),
            width = 100.dp,
            height = 56.dp
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SkeletonBox(
                width = 150.dp,
                height = 16.dp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(
                    width = 80.dp,
                    height = 12.dp
                )
                SkeletonBox(
                    width = 60.dp,
                    height = 12.dp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    width = 0.dp,
                    height = 6.dp
                )
                SkeletonBox(
                    modifier = Modifier.padding(start = 8.dp),
                    width = 30.dp,
                    height = 12.dp
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(2) {
                SkeletonBox(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    width = 24.dp,
                    height = 24.dp
                )
            }
        }
    }
}