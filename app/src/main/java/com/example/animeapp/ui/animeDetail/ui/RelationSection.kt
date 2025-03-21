package com.example.animeapp.ui.animeDetail.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.animeapp.models.AnimeDetail
import androidx.compose.foundation.rememberScrollState
import com.example.animeapp.ui.common_ui.AnimeSearchItem
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.Relation
import com.example.animeapp.ui.animeSearch.components.AnimeSearchItemSkeleton
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.basicContainer
import com.example.animeapp.utils.Navigation.navigateWithFilter

@Composable
fun RelationSection(
    navController: NavController,
    relations: List<Relation>?,
    getAnimeDetail: suspend (Int) -> Resource<AnimeDetailResponse>,
    onItemClickListener: (Int) -> Unit
) {
    if (relations != null && relations.isNotEmpty()) {
        Column(
            modifier = Modifier
                .basicContainer()
                .fillMaxWidth()
        ) {
            Text(
                text = if (relations.size > 1) "${relations.size} Relations" else "Relation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(relations) { relation ->
                    Column {
                        Text(
                            text = "${relation.entry.size} ${relation.relation}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .heightIn(max = 350.dp)
                                .widthIn(max = 350.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            relation.entry.forEach { entry ->
                                var animeDetail: AnimeDetail? by remember { mutableStateOf(null) }
                                var isLoading by remember { mutableStateOf(false) }
                                var error: String? by remember { mutableStateOf(null) }

                                LaunchedEffect(entry.mal_id) {
                                    isLoading = true
                                    when (val result = getAnimeDetail(entry.mal_id)) {
                                        is Resource.Success -> {
                                            animeDetail = result.data?.data
                                            isLoading = false
                                        }

                                        is Resource.Error -> {
                                            error = result.message
                                            isLoading = false
                                        }

                                        is Resource.Loading -> {
                                            isLoading = true
                                        }
                                    }
                                }

                                if (isLoading) {
                                    AnimeSearchItemSkeleton()
                                } else if (error != null) {
                                    AnimeSearchItem(errorTitle = entry.name)
                                } else if (animeDetail == null) {
                                    AnimeSearchItem(errorTitle = entry.name)
                                } else {
                                    AnimeSearchItem(
                                        anime = animeDetail,
                                        onGenreClick = { genre ->
                                            navigateWithFilter(
                                                navController,
                                                genre,
                                                genreFilter = true
                                            )
                                        },
                                        onItemClick = { onItemClickListener(entry.mal_id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}