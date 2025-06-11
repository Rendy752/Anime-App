package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminoverse.animevibe.models.NetworkStatus
import com.luminoverse.animevibe.ui.common.DebouncedIconButton
import com.luminoverse.animevibe.ui.theme.watchingEpisode

@Composable
fun CurrentlyWatchingHeader(
    networkStatus: NetworkStatus,
    onFavoriteToggle: () -> Unit,
    isFavorite: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        watchingEpisode
                    )
                )
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            textAlign = TextAlign.Center,
            text = "Currently Watching",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            networkStatus.let {
                Icon(
                    imageVector = it.icon,
                    contentDescription = it.label,
                    tint = it.iconColor
                )
            }
            DebouncedIconButton(
                onClick = {
                    onFavoriteToggle()
                },
                modifier = Modifier.semantics {
                    contentDescription =
                        if (isFavorite) "Remove from favorites" else "Add to favorites"
                }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}