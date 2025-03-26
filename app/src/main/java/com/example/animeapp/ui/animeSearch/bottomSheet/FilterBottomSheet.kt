package com.example.animeapp.ui.animeSearch.bottomSheet

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.animeapp.R
import com.example.animeapp.ui.animeSearch.AnimeSearchViewModel
import com.example.animeapp.ui.animeSearch.components.ApplyButton
import com.example.animeapp.ui.animeSearch.components.ResetButton
import com.example.animeapp.ui.common_ui.*
import com.example.animeapp.utils.FilterUtils

@Composable
fun FilterBottomSheet(
    viewModel: AnimeSearchViewModel,
    onDismiss: () -> Unit
) {
    val queryState by viewModel.queryState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val filterState = remember { mutableStateOf(FilterUtils.FilterState(queryState)) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        FilterHeader(viewModel, filterState, context, onDismiss)
        HorizontalDivider()
        FilterContent(scrollState, filterState)
    }
}

@Composable
private fun FilterHeader(
    viewModel: AnimeSearchViewModel,
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
            stringResource(R.string.filter_anime),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Row {
            ResetButton(
                context,
                { viewModel.queryState.value.isDefault() },
                {
                    viewModel.resetBottomSheetFilters()
                    filterState.value =
                        FilterUtils.FilterState(filterState.value.queryState.resetBottomSheetFilters())
                    onDismiss()
                }
            )
            Spacer(Modifier.width(4.dp))
            val updatedQueryState = FilterUtils.collectFilterValues(
                currentState = filterState.value.queryState,
                type = filterState.value.type,
                score = filterState.value.score?.toDoubleOrNull(),
                minScore = filterState.value.minScore?.toDoubleOrNull(),
                maxScore = filterState.value.maxScore?.toDoubleOrNull(),
                status = filterState.value.status,
                rating = filterState.value.rating,
                sfw = filterState.value.sfw,
                unapproved = filterState.value.unapproved,
                orderBy = filterState.value.orderBy,
                sort = filterState.value.sort,
                enableDateRange = filterState.value.enableDateRange,
                startDate = filterState.value.startDate,
                endDate = filterState.value.endDate
            )
            val defaultQueryState = filterState.value.queryState.resetBottomSheetFilters()
            ApplyButton(
                context,
                { updatedQueryState == defaultQueryState },
                {
                    viewModel.applyFilters(updatedQueryState)
                    onDismiss()
                }
            )
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
                    .padding(end = 4.dp)
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
        Row(Modifier.fillMaxWidth()) {
            val selectedRatingDescription by remember {
                mutableStateOf(
                    FilterUtils.getRatingDescription(
                        filterState.value.rating
                    )
                )
            }
            DropdownInputField(
                "Rating",
                FilterUtils.RATING_OPTIONS.map { FilterUtils.getRatingDescription(it) },
                selectedRatingDescription,
                { selectedRating ->
                    filterState.value =
                        filterState.value.copy(rating = FilterUtils.RATING_OPTIONS.firstOrNull {
                            FilterUtils.getRatingDescription(it) == selectedRating
                        } ?: "Any")
                },
                Modifier
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
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = filterState.value.enableDateRange,
                    onCheckedChange = {
                        filterState.value = filterState.value.copy(enableDateRange = it)
                    })
                Text("Enable Date Range")
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
                    })
            }
        }
        Row(Modifier.fillMaxWidth()) {
            CheckboxInputField(
                "Include Unapproved",
                filterState.value.unapproved,
                { filterState.value = filterState.value.copy(unapproved = it) },
                Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )
            CheckboxInputField(
                "SFW Only",
                filterState.value.sfw,
                { filterState.value = filterState.value.copy(sfw = it) },
                Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
        }
    }
}