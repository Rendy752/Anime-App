package com.luminoverse.animevibe.ui.animeDetail.numericDetail

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
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.utils.TextUtils.formatNumber

@Composable
fun NumericDetailSection(animeDetail: AnimeDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NumericDetailItem(
            title = "Score",
            value = animeDetail.score?.toString() ?: "N/A",
            subValue = animeDetail.scored_by?.let { "${it.formatNumber()} Users" } ?: "N/A",
            icon = Icons.Filled.Score
        )
        NumericDetailItem(
            title = "Ranked",
            value = animeDetail.rank?.let { "#${it.formatNumber()}" } ?: "N/A",
            icon = Icons.Filled.Star
        )
        NumericDetailItem(
            title = "Popularity",
            value = "#${animeDetail.popularity.formatNumber()}",
            icon = Icons.AutoMirrored.Filled.TrendingUp
        )
        NumericDetailItem(
            title = "Members",
            value = animeDetail.members.formatNumber(),
            icon = Icons.Filled.Groups
        )
        NumericDetailItem(
            title = "Favorites",
            value = animeDetail.favorites.formatNumber(),
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