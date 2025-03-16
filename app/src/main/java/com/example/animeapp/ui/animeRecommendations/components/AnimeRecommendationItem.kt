package com.example.animeapp.ui.animeRecommendations.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.animeapp.models.AnimeHeader
import com.example.animeapp.models.AnimeRecommendation
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.ui.tooling.preview.Preview
import com.example.animeapp.models.animeRecommendationPlaceholder
import com.example.animeapp.utils.DateUtils

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
    onItemClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                )
                .padding(16.dp)
        ) {
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
}

@Composable
private fun AnimePair(
    anime: AnimeHeader,
    isFirst: Boolean,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isImageLoading by remember { mutableStateOf(true) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClick(anime.mal_id) }
    ) {
        Text(
            text = if (isFirst) "If you like" else "Then you might like",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier.size(100.dp, 150.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = anime.images.jpg.image_url,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                onSuccess = { isImageLoading = false },
                onError = { isImageLoading = false }
            )

            if (isImageLoading) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = "Placeholder",
                    tint = Color.Gray,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(30.dp)
                )
            }
        }

        Text(
            text = anime.title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
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