package com.luminoverse.animevibe.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Score
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.sp
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.Genre
import com.luminoverse.animevibe.models.animeDetailPlaceholder
import com.luminoverse.animevibe.utils.TextUtils.formatNumber
import com.luminoverse.animevibe.utils.basicContainer

@Preview
@Composable
fun AnimeSearchItem(
    modifier: Modifier = Modifier,
    animeDetail: AnimeDetail? = animeDetailPlaceholder,
    query: String = "",
    selectedGenres: List<Genre> = emptyList(),
    errorTitle: String? = null,
    onGenreClick: ((Int) -> Unit)? = null,
    onItemClick: (() -> Unit)? = null,
    showImagePreview: ((SharedImageState) -> Unit)? = null,
) {
    val contentDescription = "Image cover of ${animeDetail?.title ?: "Anime Image"}"
    Column(
        modifier = modifier
            .basicContainer(
                outerPadding = PaddingValues(0.dp),
                onItemClick = if (onItemClick != null && animeDetail != null) {
                    onItemClick
                } else null
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (errorTitle.isNullOrEmpty()) {
                ImageDisplay(
                    modifier.weight(0.3f),
                    image = animeDetail?.images?.webp?.large_image_url,
                    contentDescription = contentDescription,
                    isAiring = animeDetail?.airing,
                    isApproved = animeDetail?.approved,
                    onClick = showImagePreview?.let {
                        { image, bounds, size ->
                            it(
                                SharedImageState(
                                    image = image,
                                    contentDescription = contentDescription,
                                    initialBounds = bounds,
                                    initialSize = size
                                )
                            )
                        }
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = "No Image",
                    modifier = Modifier
                        .size(width = 100.dp, height = 100.dp)
                        .weight(0.3f),
                )
            }
            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .padding(end = 16.dp),
            ) {
                val title = if (errorTitle.isNullOrEmpty()) animeDetail?.title
                    ?: "Unknown Title" else errorTitle
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = highlightText(title, query),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 14.sp,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
                if (errorTitle.isNullOrEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "${animeDetail?.type ?: "Unknown Type"} (${animeDetail?.episodes} eps) - ${animeDetail?.aired?.prop?.from?.year ?: "Unknown Year"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .fillMaxWidth(),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                    animeDetail?.genres?.let { genres ->
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genres.map { it }.toList().forEach { data ->
                                val isSelected = selectedGenres.any { it.mal_id == data.mal_id }
                                FilterChipView(
                                    text = data.name,
                                    checked = isSelected,
                                    onCheckedChange = {
                                        onGenreClick?.invoke(data.mal_id)
                                    }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DataTextWithIcon(
                            modifier = Modifier.weight(1f),
                            label = "Score",
                            value = animeDetail?.score.toString(),
                            icon = Icons.Filled.Score
                        )
                        DataTextWithIcon(
                            modifier = Modifier.weight(1f),
                            label = "Popularity",
                            value = animeDetail?.popularity?.formatNumber(),
                            icon = Icons.AutoMirrored.Filled.TrendingUp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DataTextWithIcon(
                            modifier = Modifier.weight(1f),
                            label = "Rank",
                            value = animeDetail?.rank?.formatNumber(),
                            icon = Icons.Filled.Star
                        )
                        DataTextWithIcon(
                            modifier = Modifier.weight(1f),
                            label = "Members",
                            value = animeDetail?.members?.formatNumber(),
                            icon = Icons.Filled.Groups
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AnimeSearchItemSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.basicContainer(outerPadding = PaddingValues(0.dp))) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SkeletonBox(width = 100.dp, height = 150.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SkeletonBox(width = 150.dp, height = 20.dp)
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonBox(width = 100.dp, height = 16.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) {
                        SkeletonBox(width = 60.dp, height = 24.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DataTextWithIconSkeleton(modifier = Modifier.weight(1f))
                    DataTextWithIconSkeleton(modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DataTextWithIconSkeleton(modifier = Modifier.weight(1f))
                    DataTextWithIconSkeleton(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}