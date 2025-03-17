package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var type by remember { mutableStateOf(viewModel.queryState.value.type ?: "Any") }
    var score by remember { mutableStateOf(viewModel.queryState.value.score?.toString() ?: "") }
    var minScore by remember { mutableStateOf(viewModel.queryState.value.minScore?.toString() ?: "") }
    var maxScore by remember { mutableStateOf(viewModel.queryState.value.maxScore?.toString() ?: "") }
    var status by remember { mutableStateOf(viewModel.queryState.value.status ?: "Any") }
    var rating by remember { mutableStateOf(viewModel.queryState.value.rating ?: "Any") }
    var sfw by remember { mutableStateOf(viewModel.queryState.value.sfw == true) }
    var unapproved by remember { mutableStateOf(viewModel.queryState.value.unapproved == true) }
    var orderBy by remember { mutableStateOf(viewModel.queryState.value.orderBy ?: "Any") }
    var sort by remember { mutableStateOf(viewModel.queryState.value.sort ?: "Any") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }

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
                style = MaterialTheme.typography.titleLarge
            )
            Row {
                Button(
                    onClick = {
                        viewModel.resetBottomSheetFilters()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(stringResource(id = R.string.reset))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = {
                        val updatedQueryState = FilterUtils.collectFilterValues(
                            currentState = viewModel.queryState.value,
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
                        viewModel.applyFilters(updatedQueryState)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
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
        ) {
            TypeFilter(viewModel)
            StatusFilter(viewModel)
            RatingFilter(viewModel)
            ScoreFilter(viewModel)
            MinMaxScoreFilter(viewModel)
            OrderBySortFilter(viewModel)
            DateRangeFilter(viewModel)
            UnapprovedFilter(viewModel)
            SfwFilter(viewModel)
        }
    }
}

@Composable
fun TypeFilter(viewModel: AnimeSearchViewModel) {
    var type by remember { mutableStateOf(viewModel.queryState.value.type ?: "Any") }
    DropdownInputField(
        label = "Type",
        options = FilterUtils.TYPE_OPTIONS,
        selectedValue = type,
        onValueChange = { type = it }
    )
}

@Composable
fun StatusFilter(viewModel: AnimeSearchViewModel) {
    var status by remember { mutableStateOf(viewModel.queryState.value.status ?: "Any") }
    DropdownInputField(
        label = "Status",
        options = FilterUtils.STATUS_OPTIONS,
        selectedValue = status,
        onValueChange = { status = it }
    )
}

@Composable
fun RatingFilter(viewModel: AnimeSearchViewModel) {
    var ratingCode by remember { mutableStateOf(viewModel.queryState.value.rating ?: "Any") }
    val selectedRatingDescription = FilterUtils.getRatingDescription(ratingCode)
    var ratingDescription by remember { mutableStateOf(selectedRatingDescription) }

    DropdownInputField(
        label = "Rating",
        options = FilterUtils.RATING_OPTIONS.map { FilterUtils.getRatingDescription(it) },
        selectedValue = ratingDescription,
        onValueChange = { selectedRating ->
            ratingDescription = selectedRating
            ratingCode = FilterUtils.RATING_OPTIONS.firstOrNull {
                FilterUtils.getRatingDescription(it) == selectedRating
            } ?: "Any"
        }
    )
}

@Composable
fun ScoreFilter(viewModel: AnimeSearchViewModel) {
    var score by remember { mutableStateOf(viewModel.queryState.value.score?.toString() ?: "") }
    NumberInputField(
        label = "Score",
        value = score,
        onValueChange = { score = it }
    )
}

@Composable
fun MinMaxScoreFilter(viewModel: AnimeSearchViewModel) {
    var minScore by remember { mutableStateOf(viewModel.queryState.value.minScore?.toString() ?: "") }
    var maxScore by remember { mutableStateOf(viewModel.queryState.value.maxScore?.toString() ?: "") }
    Row(modifier = Modifier.fillMaxWidth()) {
        NumberInputField(
            label = "Min Score",
            value = minScore,
            onValueChange = { minScore = it },
            modifier = Modifier.weight(1f).padding(end = 4.dp)
        )
        NumberInputField(
            label = "Max Score",
            value = maxScore,
            onValueChange = { maxScore = it },
            modifier = Modifier.weight(1f).padding(start = 4.dp)
        )
    }
}

@Composable
fun OrderBySortFilter(viewModel: AnimeSearchViewModel) {
    var orderBy by remember { mutableStateOf(viewModel.queryState.value.orderBy ?: "Any") }
    var sort by remember { mutableStateOf(viewModel.queryState.value.sort ?: "Any") }
    Row(modifier = Modifier.fillMaxWidth()) {
        DropdownInputField(
            label = "Order By",
            options = FilterUtils.ORDER_BY_OPTIONS,
            selectedValue = orderBy,
            onValueChange = { orderBy = it },
            modifier = Modifier.weight(1f).padding(end = 4.dp)
        )
        DropdownInputField(
            label = "Sort",
            options = FilterUtils.SORT_OPTIONS,
            selectedValue = sort,
            onValueChange = { sort = it },
            modifier = Modifier.weight(1f).padding(start = 4.dp)
        )
    }
}

@Composable
fun DateRangeFilter(viewModel: AnimeSearchViewModel) {
    var startDate by remember { mutableStateOf<LocalDate?>(viewModel.queryState.value.startDate?.let { LocalDate.parse(it) }) }
    var endDate by remember { mutableStateOf<LocalDate?>(viewModel.queryState.value.endDate?.let { LocalDate.parse(it) }) }

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
}

@Composable
fun UnapprovedFilter(viewModel: AnimeSearchViewModel) {
    var unapproved by remember { mutableStateOf(viewModel.queryState.value.unapproved == true) }
    CheckboxInputField(
        label = "Include Unapproved",
        checked = unapproved,
        onCheckedChange = { unapproved = it }
    )
}

@Composable
fun SfwFilter(viewModel: AnimeSearchViewModel) {
    var sfw by remember { mutableStateOf(viewModel.queryState.value.sfw == true) }
    CheckboxInputField(
        label = "SFW Only",
        checked = sfw,
        onCheckedChange = { sfw = it }
    )
}