package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
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
import com.luminoverse.animevibe.ui.common.CircularLoadingIndicator
import com.luminoverse.animevibe.ui.common.DebouncedIconButton
import com.luminoverse.animevibe.ui.theme.watchingEpisode
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun CurrentlyWatchingHeader(
    networkStatus: NetworkStatus,
    isError: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
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
                        MaterialTheme.colorScheme.surfaceContainerHigh, watchingEpisode
                    )
                )
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
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

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            networkStatus.let {
                Icon(
                    modifier = Modifier.padding(12.dp),
                    imageVector = it.icon,
                    contentDescription = it.label,
                    tint = it.iconColor
                )
            }
            DebouncedIconButton(
                onClick = { onFavoriteToggle() },
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
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.basicContainer(
                    isPrimary = !isError,
                    isError = isError,
                    onItemClick = { if (isRefreshing) null else onRefresh() },
                    outerPadding = PaddingValues(0.dp),
                    innerPadding = PaddingValues(4.dp)
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRefreshing) CircularLoadingIndicator()
                else Icon(
                    modifier = Modifier.size(36.dp),
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    tint = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}