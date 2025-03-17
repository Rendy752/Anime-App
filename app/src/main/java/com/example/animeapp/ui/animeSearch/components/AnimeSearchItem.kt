package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.ui.common_ui.AsyncImageWithPlaceholder
import com.example.animeapp.ui.common_ui.TitleSynonymsList

@Composable
fun AnimeSearchItem(
    anime: AnimeDetail?,
    onItemClick: ((Int) -> Unit)? = null,
) {
    anime?.let { data ->
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(16.dp).clickable { onItemClick?.invoke(data.mal_id) }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AsyncImageWithPlaceholder(
                    model = data.images.jpg.image_url,
                    contentDescription = data.title,
                    isAiring = data.airing
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = data.type.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (data.approved) {
                            Icon(
                                imageVector = Icons.Filled.Recommend,
                                contentDescription = "Approved",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    TitleSynonymsList(
                        synonyms = data.title_synonyms?.toList() ?: emptyList(),
                    )
                    DataText(label = "Score", value = data.score.toString())
                    DataText(label = "Rank", value = data.rank.toString())
                    DataText(label = "Popularity", value = data.popularity.toString())
                    DataText(label = "Members", value = data.members.toString())
                }
            }
        }
    }
}

@Composable
private fun DataText(label: String, value: String) {
    Text(text = "$label: $value", style = MaterialTheme.typography.bodyMedium)
}