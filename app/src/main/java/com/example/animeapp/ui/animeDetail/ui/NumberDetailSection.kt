package com.example.animeapp.ui.animeDetail.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Score
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animeapp.R
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.utils.TextUtils.formatNumber
import com.example.animeapp.utils.basicContainer

@Composable
fun NumberDetailSection(animeDetail: AnimeDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        NumberDetailItem(
            title = stringResource(R.string.score),
            value = animeDetail.score?.toString() ?: "N/A",
            subValue = animeDetail.scored_by?.let { "${formatNumber(it)} Users" } ?: "N/A",
            icon = Icons.Filled.Score
        )
        NumberDetailItem(
            title = stringResource(R.string.ranked),
            value = animeDetail.rank?.let { "#${formatNumber(it)}" } ?: "N/A",
            icon = Icons.Filled.Star
        )
        NumberDetailItem(
            title = stringResource(R.string.popularity),
            value = "#${formatNumber(animeDetail.popularity)}",
            icon = Icons.AutoMirrored.Filled.TrendingUp
        )
        NumberDetailItem(
            title = stringResource(R.string.members),
            value = formatNumber(animeDetail.members),
            icon = Icons.Filled.Groups
        )
        NumberDetailItem(
            title = stringResource(R.string.favorites),
            value = formatNumber(animeDetail.favorites),
            icon = Icons.Filled.Favorite
        )
    }
}

@Composable
private fun NumberDetailItem(title: String, value: String, subValue: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier
            .basicContainer()
            .height(120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        subValue?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}