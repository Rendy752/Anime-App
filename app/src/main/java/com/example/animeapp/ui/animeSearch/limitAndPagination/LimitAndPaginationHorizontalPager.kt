package com.example.animeapp.ui.animeSearch.limitAndPagination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.ui.animeSearch.components.PaginationButtons
import com.example.animeapp.ui.common_ui.DropdownInputField
import com.example.animeapp.utils.Limit

@Composable
fun LimitAndPaginationHorizontalPager(
    pagination: CompletePagination?,
    selectedLimit: Int,
    onPageChange: (Int) -> Unit,
    onLimitChange: (Int) -> Unit
) {
    if (pagination != null) HorizontalDivider()
    HorizontalPager(
        state = rememberPagerState(pageCount = { 2 }),
        Modifier.padding(8.dp)
    ) { page ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (page == 0) {
                if (pagination != null) {
                    PaginationButtons(pagination) { onPageChange(it) }
                }
            } else if (page == 1) {
                DropdownInputField(
                    label = "Limit",
                    options = Limit.limitOptions.map { it.toString() },
                    selectedValue = selectedLimit.toString(),
                    onValueChange = { onLimitChange(it.toInt()) },
                    modifier = Modifier.wrapContentSize()
                )
            }
        }
    }
}