package com.example.animeapp.ui.common_ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
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
    anime: AnimeDetail?,
    selectedGenres: List<Genre> = emptyList(),
    onGenreClick: ((Int) -> Unit)? = null,
    onItemClick: ((Int) -> Unit)? = null,
) {
    anime?.let { data ->
        Column(
            modifier = Modifier.basicContainer(onItemClick = { onItemClick?.invoke(data.mal_id) })
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AsyncImageWithPlaceholder(
                    model = data.images.jpg.image_url,
                    contentDescription = data.title,
                    isAiring = data.airing
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                ) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${data.type ?: "Unknown Type"} (${data.episodes} eps) - ${data.aired.prop.from.year ?: "Unknown Year"}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (data.approved) {
                            Icon(
                                imageVector = Icons.Filled.Recommend,
                                contentDescription = "Approved",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    data.genres?.let { genres ->
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genres.map { it }.toList().forEach { data ->
                                val isSelected = selectedGenres.any { it.mal_id == data.mal_id }
                                FilterChipView(data.name, isSelected) {
                                    onGenreClick?.invoke(data.mal_id)
                                }
                            }
                        }
                    }
                    DataTextWithIcon(
                        label = "Score",
                        value = data.score.toString(),
                        icon = Icons.Filled.Score
                    )
                    DataTextWithIcon(
                        label = "Rank",
                        value = data.rank?.let { formatNumber(it) },
                        icon = Icons.Filled.Star
                    )
                    DataTextWithIcon(
                        label = "Popularity",
                        value = formatNumber(data.popularity),
                        icon = Icons.Filled.People
                    )
                    DataTextWithIcon(
                        label = "Members",
                        value = formatNumber(data.members),
                        icon = Icons.Filled.People
                    )
                }
            }
        }
    }
}