package com.example.animeapp.ui.animeHome.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetail

@Composable
fun AnimeSchedulesGrid(
    animeSchedules: List<AnimeDetail>,
    isLandscape: Boolean,
    onItemClick: (AnimeDetail) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isLandscape) 6 else 3),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(animeSchedules) { animeDetail ->
            AnimeScheduleItem(animeDetail = animeDetail, onItemClick = onItemClick)
        }
    }
}

@Preview
@Composable
fun AnimeSchedulesGridSkeleton(isLandscape: Boolean = false) {
    val itemCount = if (isLandscape) 12 else 9

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isLandscape) 6 else 3),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(itemCount) {
            AnimeScheduleItemSkeleton()
        }
    }
}