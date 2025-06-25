package com.luminoverse.animevibe.ui.animeHome.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.AnimeSchedulesSearchQueryState
import com.luminoverse.animevibe.ui.common.FilterChipView
import com.luminoverse.animevibe.utils.TimeUtils.getDayOfWeekList

@Composable
fun FilterChipBar(
    queryState: AnimeSchedulesSearchQueryState,
    onApplyFilters: (AnimeSchedulesSearchQueryState) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(
            8.dp, Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChipView(
            text = "All",
            checked = queryState.filter == null,
            useDisabled = true,
            onCheckedChange = {
                onApplyFilters(queryState.copy(filter = null, page = 1))
            }
        )
        getDayOfWeekList().forEach { dayName ->
            FilterChipView(
                text = dayName,
                checked = dayName == queryState.filter,
                useDisabled = true,
                onCheckedChange = {
                    onApplyFilters(queryState.copy(filter = dayName, page = 1))
                }
            )
        }
    }
}