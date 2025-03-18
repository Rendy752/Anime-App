package com.example.animeapp.ui.animeRecommendations.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.ui.common_ui.SkeletonBox

@Preview
@Composable
fun AnimeRecommendationItemSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        .let { high ->
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                                .let { low ->
                                    Brush.verticalGradient(
                                        colors = listOf(high, low)
                                    )
                                }
                        }
                )
                .padding(16.dp)
        ) {
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