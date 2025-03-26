package com.example.animeapp.ui.animeDetail.numericDetail

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.animeapp.R
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.utils.TextUtils.formatNumber

@Composable
fun NumericDetailSection(animeDetail: AnimeDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        NumericDetailItem(
            title = stringResource(R.string.score),
            value = animeDetail.score?.toString() ?: "N/A",
            subValue = animeDetail.scored_by?.let { "${formatNumber(it)} Users" } ?: "N/A",
            icon = Icons.Filled.Score
        )
        NumericDetailItem(
            title = stringResource(R.string.ranked),
            value = animeDetail.rank?.let { "#${formatNumber(it)}" } ?: "N/A",
            icon = Icons.Filled.Star
        )
        NumericDetailItem(
            title = stringResource(R.string.popularity),
            value = "#${formatNumber(animeDetail.popularity)}",
            icon = Icons.AutoMirrored.Filled.TrendingUp
        )
        NumericDetailItem(
            title = stringResource(R.string.members),
            value = formatNumber(animeDetail.members),
            icon = Icons.Filled.Groups
        )
        NumericDetailItem(
            title = stringResource(R.string.favorites),
            value = formatNumber(animeDetail.favorites),
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
    ) {
        repeat(5) {
            NumericDetailItemSkeleton()
        }
    }
}