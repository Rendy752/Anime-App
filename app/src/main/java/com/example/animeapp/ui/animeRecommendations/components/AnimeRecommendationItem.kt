package com.example.animeapp.ui.animeRecommendations.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animeapp.models.AnimeHeader
import com.example.animeapp.models.AnimeRecommendation
import androidx.compose.ui.tooling.preview.Preview
import com.example.animeapp.models.animeRecommendationPlaceholder
import com.example.animeapp.ui.common_ui.AsyncImageWithPlaceholder
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.utils.DateUtils
import com.example.animeapp.utils.basicContainer
import com.example.animeapp.utils.shimmerContainer

@Preview
@Composable
fun AnimeRecommendationItemPreview() {
    AnimeRecommendationItem(
        recommendation = animeRecommendationPlaceholder,
        onItemClick = {}
    )
}

@Composable
fun AnimeRecommendationItem(
    recommendation: AnimeRecommendation,
    onItemClick: (AnimeHeader) -> Unit
) {
    Column(modifier = Modifier.basicContainer()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AnimePair(
                anime = recommendation.entry[0],
                isFirst = true,
                onItemClick = onItemClick,
                modifier = Modifier.weight(0.5f)
            )
            AnimePair(
                anime = recommendation.entry[1],
                isFirst = false,
                onItemClick = onItemClick,
                modifier = Modifier.weight(0.5f)
            )
        }
        RecommendationContent(recommendation.content)
        RecommendationDetails(recommendation)
    }
}

@Composable
private fun AnimePair(
    anime: AnimeHeader,
    isFirst: Boolean,
    onItemClick: (AnimeHeader) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .clickable { onItemClick(anime) }
    ) {
        Text(
            text = if (isFirst) "If you like" else "Then you might like",
            style = MaterialTheme.typography.titleLarge,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        AsyncImageWithPlaceholder(
            model = anime.images.jpg.image_url,
            contentDescription = anime.title,
        )

        Text(
            text = anime.title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun RecommendationContent(content: String) {
    Text(
        text = content,
        modifier = Modifier.padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Justify
    )
}

@Composable
private fun RecommendationDetails(recommendation: AnimeRecommendation) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(0.75f),
            text = "recommended by ${recommendation.user.username}",
            fontSize = 12.sp
        )
        Text(
            text = "~ ${DateUtils.formatDateToAgo(recommendation.date)}",
            fontSize = 12.sp
        )
    }
}

@Preview
@Composable
fun AnimeRecommendationItemSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.shimmerContainer()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(2) {
                    Column {
                        SkeletonBox(width = 120.dp, height = 20.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBox(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            width = 100.dp,
                            height = 150.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBox(width = 120.dp, height = 20.dp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 20.dp)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.7f), height = 20.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.55f), height = 16.dp)
                SkeletonBox(modifier = Modifier.width(60.dp), height = 16.dp)
            }
        }
    }
}