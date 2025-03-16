package com.example.animeapp.ui.common_ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.example.animeapp.models.AnimeDetail
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun AnimeSearchList(
    animes: List<AnimeDetail>,
    isLoading: Boolean,
    onItemClick: ((Int) -> Unit)? = null
) {
    if (isLoading) {
        Column {
            repeat(5) {
                AnimeSearchItem(anime = null, isLoading = true)
            }
        }
    } else {
        LazyColumn {
            items(animes) { anime ->
                AnimeSearchItem(anime = anime, isLoading = false, onItemClick = onItemClick)
            }
        }
    }
}