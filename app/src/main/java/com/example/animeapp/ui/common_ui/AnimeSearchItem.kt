package com.example.animeapp.ui.common_ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Score
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.CommonIdentity
import com.example.animeapp.models.Genre
import com.example.animeapp.models.animeDetailPlaceholder
import com.example.animeapp.utils.TextUtils.formatNumber
import com.example.animeapp.utils.basicContainer

@Preview
@Composable
fun AnimeSearchItemPreview() {
    AnimeSearchItem(
        anime = animeDetailPlaceholder,
        onItemClick = {}
    )
}

@Composable
fun AnimeSearchItem(
    anime: AnimeDetail? = null,
    selectedGenres: List<Genre> = emptyList(),
    errorTitle: String? = null,
    onGenreClick: ((CommonIdentity) -> Unit)? = null,
    onItemClick: ((Int) -> Unit)? = null,
) {
    val modifier = if (onItemClick != null && anime != null) {
        Modifier.basicContainer(onItemClick = { onItemClick.invoke(anime.mal_id) })
    } else Modifier.basicContainer()
    Column(
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (errorTitle.isNullOrEmpty()) {
                AsyncImageWithPlaceholder(
                    model = anime?.images?.jpg?.image_url,
                    contentDescription = anime?.title,
                    isAiring = anime?.airing
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = "No Image",
                    modifier = Modifier
                        .size(width = 100.dp, height = 100.dp),

                    )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
            ) {
                Text(
                    text = if (errorTitle.isNullOrEmpty()) anime?.title
                        ?: "Unknown Title" else errorTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (errorTitle.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${anime?.type ?: "Unknown Type"} (${anime?.episodes} eps) - ${anime?.aired?.prop?.from?.year ?: "Unknown Year"}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (anime?.approved == true) {
                            Icon(
                                imageVector = Icons.Filled.Recommend,
                                contentDescription = "Approved",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    anime?.genres?.let { genres ->
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genres.map { it }.toList().forEach { data ->
                                val isSelected = selectedGenres.any { it.mal_id == data.mal_id }
                                FilterChipView(data.name, isSelected) {
                                    onGenreClick?.invoke(data)
                                }
                            }
                        }
                    }
                    DataTextWithIcon(
                        label = "Score",
                        value = anime?.score.toString(),
                        icon = Icons.Filled.Score
                    )
                    DataTextWithIcon(
                        label = "Rank",
                        value = anime?.rank?.let { formatNumber(it) },
                        icon = Icons.Filled.Star
                    )
                    DataTextWithIcon(
                        label = "Popularity",
                        value = formatNumber(anime?.popularity ?: 0),
                        icon = Icons.AutoMirrored.Filled.TrendingUp
                    )
                    DataTextWithIcon(
                        label = "Members",
                        value = formatNumber(anime?.members ?: 0),
                        icon = Icons.Filled.Groups
                    )
                }
            }
        }
    }
}