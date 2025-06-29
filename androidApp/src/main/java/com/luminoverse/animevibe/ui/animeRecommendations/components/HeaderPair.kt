package com.luminoverse.animevibe.ui.animeRecommendations.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminoverse.animevibe.models.AnimeHeader
import com.luminoverse.animevibe.ui.common.ImageDisplay
import com.luminoverse.animevibe.ui.common.SkeletonBox

@Composable
fun HeaderPair(
    anime: AnimeHeader,
    isFirst: Boolean,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .clickable { onItemClick(anime.mal_id) }
    ) {
        Text(
            text = if (isFirst) "If you like" else "Then you might like",
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ImageDisplay(
            modifier = Modifier.padding(horizontal = 24.dp),
            image = anime.images.webp.large_image_url,
            contentDescription = anime.title,
        )

        Text(
            text = anime.title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun HeaderPairSkeleton(isFirst: Boolean, modifier: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = if (isFirst) "If you like" else "Then you might like",
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        SkeletonBox(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            width = 120.dp,
            height = 180.dp
        )
        Spacer(modifier = Modifier.height(4.dp))
        SkeletonBox(width = 140.dp, height = 20.dp)
    }
}