package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.utils.shimmerContainer

@Preview
@Composable
fun AnimeSearchItemSkeleton() {
    Column(
        modifier = Modifier
            .shimmerContainer()
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SkeletonBox(width = 80.dp, height = 120.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
            ) {
                SkeletonBox(width = 150.dp, height = 20.dp)
                Spacer(modifier = Modifier.height(2.dp))
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
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(width = 90.dp, height = 16.dp)
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonBox(width = 85.dp, height = 16.dp)
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonBox(width = 100.dp, height = 16.dp)
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonBox(width = 110.dp, height = 16.dp)
            }
        }
    }
}