package com.luminoverse.animevibe.ui.animeSearch.bottomSheet

import android.content.Context
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.AnimeSearchQueryState
import com.luminoverse.animevibe.ui.animeSearch.components.ApplyButton
import com.luminoverse.animevibe.ui.animeSearch.components.ResetButton
import com.luminoverse.animevibe.ui.common_ui.*
import com.luminoverse.animevibe.utils.FilterUtils

@Composable
fun FilterBottomSheet(
    queryState: AnimeSearchQueryState,
    applyFilters: (AnimeSearchQueryState) -> Unit,
    resetBottomSheetFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val filterState = remember { mutableStateOf(FilterUtils.FilterState(queryState)) }

    Column(Modifier.fillMaxWidth()) {
        FilterHeader(
            queryState,
            applyFilters,
            resetBottomSheetFilters,
            filterState,
            context,
            onDismiss
        )
        HorizontalDivider()
        FilterContent(scrollState, filterState)
    }
}

@Composable
private fun FilterHeader(
    queryState: AnimeSearchQueryState,
    applyFilters: (AnimeSearchQueryState) -> Unit,
    resetBottomSheetFilters: () -> Unit,
    filterState: MutableState<FilterUtils.FilterState>,
    context: Context,
    onDismiss: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Filter",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Row {
            ResetButton(
                context,
                { queryState.isDefault() },
                {
                    resetBottomSheetFilters()
                    filterState.value =
                        FilterUtils.FilterState(filterState.value.queryState.resetBottomSheetFilters())
                    onDismiss()
                }
            )
            Spacer(Modifier.width(4.dp))
            filterState.value.apply {
                val updatedQueryState = FilterUtils.collectFilterValues(
                    currentState = queryState,
                    type = type,
                    score = score?.toDoubleOrNull(),
                    minScore = minScore?.toDoubleOrNull(),
                    maxScore = maxScore?.toDoubleOrNull(),
                    status = status,
                    rating = rating,
                    sfw = sfw,
                    unapproved = unapproved,
                    orderBy = orderBy,
                    sort = sort,
                    enableDateRange = enableDateRange,
                    startDate = startDate,
                    endDate = endDate
                )
                val defaultQueryState = queryState.resetBottomSheetFilters()
                ApplyButton(
                    context,
                    { updatedQueryState == defaultQueryState },
                    {
                        applyFilters(updatedQueryState)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterContent(
    scrollState: ScrollState,
    filterState: MutableState<FilterUtils.FilterState>,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .verticalScroll(scrollState)
    ) {
        Row(Modifier.fillMaxWidth()) {
            DropdownInputField(
                "Type",
                FilterUtils.TYPE_OPTIONS,
                filterState.value.type,
                { filterState.value = filterState.value.copy(type = it) },
                Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                false
            )
            DropdownInputField(
                "Status",
                FilterUtils.STATUS_OPTIONS,
                filterState.value.status,
                { filterState.value = filterState.value.copy(status = it) },
                Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            DropdownInputField(
                label = "Rating",
                options = FilterUtils.RATING_OPTIONS.values.toList(),
                selectedValue = FilterUtils.RATING_OPTIONS[filterState.value.rating]
                    ?: filterState.value.rating,
                onValueChange = { selectedDescription ->
                    val selectedKey = FilterUtils.RATING_OPTIONS.entries.firstOrNull {
                        it.value == selectedDescription
                    }?.key ?: filterState.value.rating
                    filterState.value = filterState.value.copy(rating = selectedKey)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )
            NumberInputField(
                "Score",
                filterState.value.score ?: "",
                { filterState.value = filterState.value.copy(score = it) },
                Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                minValue = 0.0,
                maxValue = 10.0
            )
        }

        Row(Modifier.fillMaxWidth()) {
            NumberInputField(
                "Min Score",
                filterState.value.minScore ?: "",
                { filterState.value = filterState.value.copy(minScore = it) },
                Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                minValue = 0.0,
                maxValue = 10.0
            )
            NumberInputField(
                "Max Score",
                filterState.value.maxScore ?: "",
                { filterState.value = filterState.value.copy(maxScore = it) },
                Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                minValue = 0.0,
                maxValue = 10.0
            )
        }

        Row(Modifier.fillMaxWidth()) {
            DropdownInputField(
                "Order By",
                FilterUtils.ORDER_BY_OPTIONS,
                filterState.value.orderBy,
                { filterState.value = filterState.value.copy(orderBy = it) },
                Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )
            DropdownInputField(
                "Sort",
                FilterUtils.SORT_OPTIONS,
                filterState.value.sort,
                { filterState.value = filterState.value.copy(sort = it) },
                Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = filterState.value.enableDateRange,
                        onCheckedChange = {
                            filterState.value = filterState.value.copy(enableDateRange = it)
                        }
                    )
                    Text("Enable Date Range")
                }
                CheckboxInputField(
                    "Include Unapproved",
                    filterState.value.unapproved,
                    { filterState.value = filterState.value.copy(unapproved = it) },
                    Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
            }
            if (filterState.value.enableDateRange) {
                DateRangePickerInline(
                    filterState.value.startDate,
                    filterState.value.endDate,
                    { newDateRange ->
                        filterState.value = filterState.value.copy(
                            startDate = newDateRange.first,
                            endDate = newDateRange.second
                        )
                    },
                    {
                        filterState.value = filterState.value.copy(startDate = null, endDate = null)
                    }
                )
            }
        }
    }
}