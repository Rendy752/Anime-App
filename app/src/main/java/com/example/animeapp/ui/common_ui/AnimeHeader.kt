package com.example.animeapp.ui.common_ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import com.example.animeapp.models.AnimeDetail

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimeHeader(animeDetail: AnimeDetail) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImageWithPlaceholder(
            model = animeDetail.images.jpg.large_image_url,
            contentDescription = animeDetail.title,
            isAiring = animeDetail.airing,
            modifier = Modifier
                .weight(0.5f)
                .aspectRatio(2f / 3f)
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

                if (animeDetail.approved == true) {
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
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = animeDetail.title_english ?: "",
                style = MaterialTheme.typography.bodyMedium,
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
fun AnimeHeaderSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBox(width = 200.dp, height = 300.dp)

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