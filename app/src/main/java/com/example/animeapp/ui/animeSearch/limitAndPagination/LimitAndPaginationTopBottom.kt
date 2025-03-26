package com.example.animeapp.ui.animeSearch.limitAndPagination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.ui.animeSearch.components.PaginationButtons
import com.example.animeapp.ui.common_ui.DropdownInputField
import com.example.animeapp.utils.Limit

@Composable
fun LimitAndPaginationTopBottom(
    pagination: CompletePagination?,
    selectedLimit: Int,
    onPageChange: (Int) -> Unit,
    onLimitChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (pagination != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaginationButtons(pagination) { onPageChange(it) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
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