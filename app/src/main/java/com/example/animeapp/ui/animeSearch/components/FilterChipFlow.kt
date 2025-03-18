package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.ui.common_ui.FilterChipView

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipFlow(
    itemList: List<Any>,
    onSetSelectedId: (Int) -> Unit,
    itemName: (Any) -> String,
    getItemId: (Any) -> Int,
    isHorizontal: Boolean = false,
    isChecked: Boolean = false
) {
    val itemScrollState = rememberScrollState()
    val modifier: Modifier = if (isHorizontal) {
        Modifier
            .fillMaxWidth()
            .horizontalScroll(itemScrollState)
    } else {
        Modifier
            .fillMaxWidth()
            .verticalScroll(itemScrollState)
    }
    FlowRow(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(
            4.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemList.forEach { item ->
            FilterChipView(
                text = itemName(item),
                checked = isChecked,
                onCheckedChange = { onSetSelectedId(getItemId(item)) }
            )
        }
    }
}