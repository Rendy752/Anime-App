package com.luminoverse.animevibe.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.animeDetailPlaceholder
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun AnimeHeader(
    modifier: Modifier = Modifier,
    animeDetail: AnimeDetail = animeDetailPlaceholder,
    showImage: Boolean = true,
    navController: NavController? = null
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showImage) ImageDisplay(
            modifier = Modifier.weight(0.5f),
            image = animeDetail.images.webp.large_image_url,
            contentDescription = animeDetail.title,
            isAiring = animeDetail.airing
        )

        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(0.5f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = animeDetail.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = {
                                navController?.navigateTo(
                                    NavRoute.AnimeDetail.fromId(animeDetail.mal_id)
                                )
                            },
                            onDoubleClick = {
                                val intent = Intent(Intent.ACTION_VIEW, animeDetail.url.toUri())
                                context.startActivity(intent)
                            },
                            onLongClick = {
                                val clipboard = ContextCompat.getSystemService(
                                    context,
                                    ClipboardManager::class.java
                                )
                                val clip = ClipData.newPlainText(
                                    "Anime Title",
                                    animeDetail.title
                                )
                                clipboard?.setPrimaryClip(clip)
                            }
                        )
                )

                if (animeDetail.approved) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Approved",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Text(
                text = animeDetail.title_japanese ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = animeDetail.title_english ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (!animeDetail.title_synonyms.isNullOrEmpty()) {
                HorizontalScrollChipList(dataList = animeDetail.title_synonyms.toList())
            }
        }
    }
}

@Preview
@Composable
fun AnimeHeaderSkeleton(modifier: Modifier = Modifier, showImage: Boolean = true) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showImage) SkeletonBox(width = 200.dp, height = 300.dp)

        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .weight(1f),
                    height = 24.dp
                )

                SkeletonBox(
                    modifier = Modifier.size(24.dp),
                    height = 24.dp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            SkeletonBox(
                modifier = Modifier.fillMaxWidth(0.8f),
                height = 20.dp
            )

            Spacer(modifier = Modifier.height(4.dp))
            SkeletonBox(
                modifier = Modifier.fillMaxWidth(0.6f),
                height = 20.dp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) {
                    SkeletonBox(width = 60.dp, height = 24.dp)
                }
            }
        }
    }
}