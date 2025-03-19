package com.example.animeapp.ui.common_ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.animeapp.R
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
        AsyncImage(
            model = animeDetail.images.jpg.large_image_url,
            contentDescription = "Anime Image",
            modifier = Modifier
                .width(200.dp)
                .height(300.dp),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = animeDetail.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Approved",
                        tint = MaterialTheme.colorScheme.primary
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

            val airedStatusIcon = if (animeDetail.airing == true) {
                R.drawable.ic_notifications_active_24dp
            } else {
                R.drawable.ic_done_24dp
            }

            Icon(
                painter = androidx.compose.ui.res.painterResource(id = airedStatusIcon),
                contentDescription = "Aired Status",
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}