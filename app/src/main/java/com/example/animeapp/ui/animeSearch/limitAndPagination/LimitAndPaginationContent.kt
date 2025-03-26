package com.example.animeapp.ui.animeSearch.limitAndPagination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
fun LimitAndPaginationContent(
    page: Int,
    paginationState: CompletePagination?,
    selectedLimit: Int,
    onPageChange: (Int) -> Unit,
    onLimitChange: (Int) -> Unit,
    isPager: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPager && page == 0 || !isPager) {
            if (paginationState != null) {
                PaginationButtons(paginationState, onPageChange)
            }
        }
        if (isPager && page == 1 || !isPager) {
            DropdownInputField(
                label = "Limit",
                options = Limit.limitOptions.map { it.toString() },
                selectedValue = selectedLimit.toString(),
                onValueChange = { onLimitChange(it.toInt()) },
                modifier = Modifier.wrapContentSize()
            )
        }
    }
    if (!isPager) {
        Spacer(modifier = Modifier.height(8.dp))
    }
}