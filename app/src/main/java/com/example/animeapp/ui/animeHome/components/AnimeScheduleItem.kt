package com.example.animeapp.ui.animeHome.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.ui.common_ui.AsyncImageWithPlaceholder
import com.example.animeapp.ui.common_ui.ImageRoundedCorner
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.utils.basicContainer

@Composable
fun AnimeScheduleItem(
    animeDetail: AnimeDetail,
    remainingTime: String,
    onItemClick: (AnimeDetail) -> Unit
) {
    Column(
        modifier = Modifier
            .basicContainer(
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(0.dp),
                onItemClick = { onItemClick(animeDetail) })
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
                            )
                        )
                        .align(Alignment.TopStart),
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (remainingTime.isNotEmpty()) {
                Text(
                    text = remainingTime,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .basicContainer(
                            isError = remainingTime != "On Air",
                            isPrimary = remainingTime == "On Air",
                            innerPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                        )
                )
            }
        }
        Text(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            text = animeDetail.title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
fun AnimeScheduleItemSkeleton() {
    Column(
        modifier = Modifier
            .basicContainer(
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(0.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                SkeletonBox(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    width = 30.dp,
                    height = 20.dp
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                SkeletonBox(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    width = 75.dp,
                    height = 20.dp
                )
            }
        }
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            height = 40.dp
        )
    }
}