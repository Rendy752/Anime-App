package com.luminoverse.animevibe.ui.animeHome.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.ui.common.AnimeScheduleItem
import com.luminoverse.animevibe.ui.common.AnimeScheduleItemSkeleton

@Composable
fun AnimeSchedulesGrid(
    animeSchedules: List<AnimeDetail>,
    remainingTimes: Map<String, String>,
    isLandscape: Boolean,
    onItemClick: (AnimeDetail) -> Unit,
    gridState: LazyGridState
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isLandscape) 6 else 3),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        state = gridState
    ) {
        items(animeSchedules, key = { it.mal_id }) { animeDetail ->
            AnimeScheduleItem(
                animeDetail = animeDetail,
                remainingTime = remainingTimes[animeDetail.mal_id.toString()] ?: "",
                onItemClick = onItemClick
            )
        }
    }
}

@Preview
@Composable
fun AnimeSchedulesGridSkeleton(
    gridState: LazyGridState = LazyGridState(),
    isLandscape: Boolean = false
) {
    val itemCount = if (isLandscape) 12 else 9

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isLandscape) 6 else 3),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        state = gridState
    ) {
        items(itemCount) {
            AnimeScheduleItemSkeleton()
        }
    }
}