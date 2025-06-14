package com.luminoverse.animevibe.ui.animeSearch.searchField

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.Producer
import com.luminoverse.animevibe.ui.common.FilterChipViewSkeleton
import com.luminoverse.animevibe.ui.common.FilterChipView

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipFlow(
    itemList: List<Any>,
    onSetSelectedId: (Any) -> Unit,
    itemName: (Any) -> String,
    getItemId: (Any) -> Any,
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
                imageUrl = if (item is Producer) item.images?.jpg?.image_url else null,
                onCheckedChange = { onSetSelectedId(getItemId(item)) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview
@Composable
fun FilterChipFlowSkeleton(
    count: Int = 10,
    isHorizontal: Boolean = false
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
            12.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) {
            FilterChipViewSkeleton()
        }
    }
}