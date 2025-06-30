package com.luminoverse.animevibe.ui.animeRecommendations.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminoverse.animevibe.models.AnimeRecommendation
import androidx.compose.ui.tooling.preview.Preview
import com.luminoverse.animevibe.models.animeRecommendationPlaceholder
import com.luminoverse.animevibe.ui.common.SkeletonBox
import com.luminoverse.animevibe.utils.TimeUtils
import com.luminoverse.animevibe.utils.basicContainer

@Preview
@Composable
fun RecommendationItem(
    modifier: Modifier = Modifier,
    recommendation: AnimeRecommendation =  animeRecommendationPlaceholder,
    onItemClick: (Int) -> Unit = {}
) {
    Column(modifier = modifier.basicContainer(outerPadding = PaddingValues(0.dp))) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HeaderPair(
                anime = recommendation.entry[0],
                isFirst = true,
                onItemClick = onItemClick,
                modifier = Modifier.weight(0.5f)
            )
            HeaderPair(
                anime = recommendation.entry[1],
                isFirst = false,
                onItemClick = onItemClick,
                modifier = Modifier.weight(0.5f)
            )
        }
        Text(
            text = recommendation.content,
            modifier = Modifier.padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Justify
        )
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
                text = "~ ${TimeUtils.formatDateToAgo(recommendation.date)}",
                fontSize = 12.sp
            )
        }
    }
}

@Preview
@Composable
fun RecommendationItemSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.basicContainer(outerPadding = PaddingValues(0.dp))) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(2) {
                HeaderPairSkeleton(isFirst = it == 0, modifier = Modifier.weight(0.5f))
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