package com.luminoverse.animevibe.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Score
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.utils.TextUtils.formatNumber

@Composable
fun NumericDetailSection(
    score: Double?,
    scoredBy: Int?,
    rank: Int?,
    popularity: Int,
    members: Int,
    favorites: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        score?.let {
            NumericDetailItem(
                title = "Score",
                value = it.toString(),
                subValue = scoredBy?.let { "${it.formatNumber()} Users" } ?: "N/A",
                icon = Icons.Filled.Score
            )
        }
        rank?.let {
            NumericDetailItem(
                title = "Ranked",
                value = "#${it.formatNumber()}",
                icon = Icons.Filled.Star
            )
        }
        NumericDetailItem(
            title = "Popularity",
            value = "#${popularity.formatNumber()}",
            icon = Icons.AutoMirrored.Filled.TrendingUp
        )
        NumericDetailItem(
            title = "Members",
            value = members.formatNumber(),
            icon = Icons.Filled.Groups
        )
        NumericDetailItem(
            title = "Favorites",
            value = favorites.formatNumber(),
            icon = Icons.Filled.Favorite
        )
    }
}

@Preview
@Composable
fun NumericDetailSectionSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(5) {
            NumericDetailItemSkeleton()
        }
    }
}