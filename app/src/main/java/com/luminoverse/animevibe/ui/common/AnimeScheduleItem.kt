package com.luminoverse.animevibe.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.luminoverse.animevibe.models.animeDetailPlaceholder
import com.luminoverse.animevibe.utils.basicContainer

@Preview
@Composable
fun AnimeScheduleItem(
    modifier: Modifier = Modifier,
    animeDetail: AnimeDetail = animeDetailPlaceholder,
    remainingTime: String = "23h 59m",
    onItemClick: ((AnimeDetail) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .basicContainer(
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(0.dp),
                onItemClick = onItemClick?.let { { it(animeDetail) } }
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImageWithPlaceholder(
                model = animeDetail.images.webp.large_image_url,
                contentDescription = animeDetail.title,
                roundedCorners = ImageRoundedCorner.TOP,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
                isClickable = false
            )
            animeDetail.type?.let {
                Text(
                    modifier = Modifier
                        .basicContainer(
                            innerPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                            alpha = 0.75f
                        )
                        .align(Alignment.TopStart),
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                if (remainingTime.isNotEmpty()) {
                    Text(
                        text = remainingTime,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.End)
                            .basicContainer(
                                isError = remainingTime != "On Air",
                                isPrimary = remainingTime == "On Air",
                                innerPadding = PaddingValues(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                ),
                                alpha = 0.75f
                            )
                    )
                }
                Text(
                    modifier = Modifier
                        .basicContainer(
                            outerPadding = PaddingValues(0.dp),
                            innerPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                            alpha = 0.75f
                        )
                        .fillMaxWidth(),
                    text = animeDetail.title,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview
@Composable
fun AnimeScheduleItemSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .basicContainer(
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(0.dp)
            )
            .fillMaxWidth()
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        )
        SkeletonBox(
            modifier = Modifier
                .basicContainer(
                    innerPadding = PaddingValues(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    ),
                    alpha = 0.75f
                )
                .align(Alignment.TopStart),
            width = 30.dp,
            height = 20.dp
        )
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            Column(horizontalAlignment = Alignment.End) {
                SkeletonBox(
                    modifier = Modifier
                        .basicContainer(
                            isPrimary = true,
                            innerPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                            alpha = 0.75f
                        ),
                    width = 75.dp,
                    height = 20.dp
                )
                SkeletonBox(
                    modifier = Modifier
                        .basicContainer(
                            outerPadding = PaddingValues(0.dp),
                            innerPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                            alpha = 0.75f
                        )
                        .fillMaxWidth(),
                    height = 40.dp
                )
            }
        }
    }
}