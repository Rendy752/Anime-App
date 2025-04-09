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
fun AnimeSeasonNowGrid(
    animeSeasonNow: List<AnimeDetail>,
    isLandscape: Boolean,
    onItemClick: (AnimeDetail) -> Unit
) {
    val gridCellsCount = if (isLandscape) 4 else 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridCellsCount),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(animeSeasonNow) { animeDetail ->
            AnimeSeasoNowItem(animeDetail = animeDetail, onItemClick = onItemClick)
        }
    }
}

@Preview
@Composable
fun AnimeSeasonNowGridSkeleton(isLandscape: Boolean = false) {
    val itemCount = if (isLandscape) 8 else 6

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isLandscape) 4 else 2),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(itemCount) {
            AnimeSeasoNowItemSkeleton()
        }
    }
}