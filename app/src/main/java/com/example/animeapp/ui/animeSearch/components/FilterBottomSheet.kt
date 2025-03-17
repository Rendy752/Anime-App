package com.example.animeapp.ui.animeSearch.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.ui.common_ui.*
import com.example.animeapp.utils.FilterUtils
import java.time.LocalDate

@Composable
fun FilterBottomSheet(
    viewModel: AnimeSearchViewModel,
    onDismiss: () -> Unit
) {
    val queryState by viewModel.queryState.collectAsState()

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var type by remember { mutableStateOf(queryState.type ?: "Any") }
    var score by remember { mutableStateOf(queryState.score?.toString() ?: "") }
    var minScore by remember { mutableStateOf(queryState.minScore?.toString() ?: "") }
    var maxScore by remember { mutableStateOf(queryState.maxScore?.toString() ?: "") }
    var status by remember { mutableStateOf(queryState.status ?: "Any") }
    var rating by remember { mutableStateOf(queryState.rating ?: "Any") }
    var sfw by remember { mutableStateOf(queryState.sfw == true) }
    var unapproved by remember { mutableStateOf(queryState.unapproved == true) }
    var orderBy by remember { mutableStateOf(queryState.orderBy ?: "Any") }
    var sort by remember { mutableStateOf(queryState.sort ?: "Any") }
    var startDate by remember {
        mutableStateOf<LocalDate?>(queryState.startDate?.let {
            LocalDate.parse(
                it
            )
        })
    }
    var endDate by remember {
        mutableStateOf<LocalDate?>(viewModel.queryState.value.endDate?.let {
            LocalDate.parse(
                it
            )
        })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.filter_anime),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row {
                Button(
                    onClick = {
                        if (queryState.isDefault()) {
                            showToast(context, "Filters are already default")
                        } else {
                            viewModel.resetBottomSheetFilters()
                            onDismiss()
                        }
                    },
                    enabled = !queryState.isDefault(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                ) {
                    Text(stringResource(id = R.string.reset))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = {
                        val updatedQueryState = FilterUtils.collectFilterValues(
                            currentState = queryState,
                            type = type,
                            score = score.toDoubleOrNull(),
                            minScore = minScore.toDoubleOrNull(),
                            maxScore = maxScore.toDoubleOrNull(),
                            status = status,
                            rating = rating,
                            sfw = sfw,
                            unapproved = unapproved,
                            orderBy = orderBy,
                            sort = sort,
                            startDate = startDate,
                            endDate = endDate
                        )
                        val defaultQueryState = queryState.resetBottomSheetFilters()
                        if (updatedQueryState == defaultQueryState) {
                            showToast(context, "No filters applied, you can reset")
                        } else {
                            viewModel.applyFilters(updatedQueryState)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(id = R.string.apply))
                }
            }
        }
        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                DropdownInputField(
                    label = "Type",
                    options = FilterUtils.TYPE_OPTIONS,
                    selectedValue = type,
                    onValueChange = { type = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )
                DropdownInputField(
                    label = "Status",
                    options = FilterUtils.STATUS_OPTIONS,
                    selectedValue = status,
                    onValueChange = { status = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                val selectedRatingDescription = FilterUtils.getRatingDescription(rating)
                var ratingDescription by remember { mutableStateOf(selectedRatingDescription) }
                DropdownInputField(
                    label = "Rating",
                    options = FilterUtils.RATING_OPTIONS.map { FilterUtils.getRatingDescription(it) },
                    selectedValue = ratingDescription,
                    onValueChange = { selectedRating ->
                        ratingDescription = selectedRating
                        rating = FilterUtils.RATING_OPTIONS.firstOrNull {
                            FilterUtils.getRatingDescription(it) == selectedRating
                        } ?: "Any"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )
                NumberInputField(
                    label = "Score",
                    value = score,
                    onValueChange = { score = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    minValue = 0.0,
                    maxValue = 10.0
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                NumberInputField(
                    label = "Min Score",
                    value = minScore,
                    onValueChange = { minScore = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    minValue = 0.0,
                    maxValue = 10.0
                )
                NumberInputField(
                    label = "Max Score",
                    value = maxScore,
                    onValueChange = { maxScore = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    minValue = 0.0,
                    maxValue = 10.0
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                DropdownInputField(
                    label = "Order By",
                    options = FilterUtils.ORDER_BY_OPTIONS,
                    selectedValue = orderBy,
                    onValueChange = { orderBy = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )
                DropdownInputField(
                    label = "Sort",
                    options = FilterUtils.SORT_OPTIONS,
                    selectedValue = sort,
                    onValueChange = { sort = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Date Range")
                Row {
                    Text(
                        text = startDate?.toString() ?: "Start Date",
                        modifier = Modifier.clickable { /* Implement date picker */ }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = endDate?.toString() ?: "End Date",
                        modifier = Modifier.clickable { /* Implement date picker */ }
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                CheckboxInputField(
                    label = "Include Unapproved",
                    checked = unapproved,
                    onCheckedChange = { unapproved = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)

                )
                CheckboxInputField(
                    label = "SFW Only",
                    checked = sfw,
                    onCheckedChange = { sfw = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
            }
        }
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}