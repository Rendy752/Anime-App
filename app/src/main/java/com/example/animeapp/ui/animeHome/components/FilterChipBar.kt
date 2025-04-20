package com.example.animeapp.ui.animeHome.components

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
import com.example.animeapp.ui.animeHome.HomeAction
import com.example.animeapp.ui.animeHome.HomeState
import com.example.animeapp.ui.common_ui.FilterChipView
import com.example.animeapp.utils.TimeUtils.getDayOfWeekList

@Composable
fun FilterChipBar(state: HomeState, action: (HomeAction) -> Unit) {
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
            checked = state.queryState.filter == null,
            useDisabled = true,
            onCheckedChange = {
                action(
                    HomeAction.ApplyFilters(
                        state.queryState.copy(filter = null, page = 1)
                    )
                )
            }
        )
        getDayOfWeekList().forEach { dayName ->
            FilterChipView(
                text = dayName,
                checked = dayName == state.queryState.filter,
                useDisabled = true,
                onCheckedChange = {
                    action(
                        HomeAction.ApplyFilters(
                            state.queryState.copy(filter = dayName, page = 1)
                        )
                    )
                }
            )
        }
    }
}