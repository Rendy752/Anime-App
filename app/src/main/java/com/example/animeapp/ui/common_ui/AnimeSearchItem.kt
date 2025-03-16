package com.example.animeapp.ui.common_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.animeapp.R
import com.example.animeapp.models.AnimeDetail

@Composable
fun AnimeSearchItem(
    anime: AnimeDetail?,
    isLoading: Boolean,
    onItemClick: ((Int) -> Unit)? = null
) {
    if (isLoading) {
        AnimeSearchItemShimmer()
    } else {
        anime?.let { data ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { onItemClick?.invoke(data.mal_id) }
            ) {
                Row {
                    AsyncImage(
                        model = data.images.jpg.image_url,
                        contentDescription = "Anime Image",
                        modifier = Modifier
                            .size(100.dp, 150.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_error_yellow_24dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = data.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = data.type.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (data.approved) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_recommend_blue_24dp),
                                    contentDescription = "Approved",
                                    tint = Color.Blue
                                )
                            }
                        }
                        TitleSynonymsList(synonyms = data.title_synonyms?.toList() ?: emptyList())
                        Text(text = "Score: ${data.score}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Rank: ${data.rank}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Popularity: ${data.popularity}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Members: ${data.members}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeSearchItemShimmer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(100.dp, 150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Add more shimmer placeholders as needed
            }
        }
    }
}