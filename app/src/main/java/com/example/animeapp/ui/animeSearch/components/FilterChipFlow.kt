package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.animeapp.ui.common_ui.FilterChipView

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipFlow(
    selectedList: List<Any>,
    unselectedList: List<Any>,
    selectedIds: List<Int>,
    onSetSelectedId: (Int) -> Unit,
    itemName: (Any) -> String,
    getItemId: (Any) -> Int,
    noSelectedItemMessage: String
) {
    val selectedScrollState = rememberScrollState()
    val unselectedScrollState = rememberScrollState()

    if (selectedIds.isNotEmpty()) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(selectedScrollState)
                .padding(end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(
                4.dp,
                alignment = Alignment.CenterHorizontally
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            selectedList.forEach { item ->
                FilterChipView(
                    text = itemName(item),
                    checked = true,
                    onCheckedChange = { onSetSelectedId(getItemId(item)) }
                )
            }
        }
    } else {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
            text = noSelectedItemMessage
        )
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(unselectedScrollState),
        horizontalArrangement = Arrangement.spacedBy(
            4.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        unselectedList.forEach { item ->
            FilterChipView(
                text = itemName(item),
                checked = false,
                onCheckedChange = { onSetSelectedId(getItemId(item)) }
            )
        }
    }
}