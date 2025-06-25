package com.luminoverse.animevibe.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.utils.TextUtils.toTitleCase
import java.time.LocalDate

@Composable
fun NumberInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minValue: Double? = null,
    maxValue: Double? = null
) {
    var isError by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        isError = isError,
        supportingText = {
            if (isError && minValue != null && maxValue != null)
                Text(text = "Value must be between $minValue and $maxValue")
        },
        onValueChange = { newValue ->
            isError = false
            val doubleValue = newValue.toDoubleOrNull()
            if (doubleValue != null) {
                if ((minValue == null || doubleValue >= minValue) &&
                    (maxValue == null || doubleValue <= maxValue)
                ) {
                    onValueChange(newValue)
                } else {
                    isError = true
                }
            } else if (newValue.isEmpty()) {
                onValueChange(newValue)
            } else {
                isError = true
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownInputField(
    label: String,
    options: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isFormatLabel: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            readOnly = true,
            value = if (isFormatLabel) selectedValue.toTitleCase() else selectedValue,
            onValueChange = {},
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                    enabled = true
                )
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .exposedDropdownSize(matchAnchorWidth = true)
                .fillMaxWidth()
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (isFormatLabel) option.toTitleCase() else option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CheckboxInputField(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(text = label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerInline(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateRangeSelected: (Pair<LocalDate?, LocalDate?>) -> Unit,
    onReset: () -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startDate?.toEpochDay()?.let { it * 86400000 },
        initialSelectedEndDateMillis = endDate?.toEpochDay()?.let { it * 86400000 }
    )

    Column(Modifier.fillMaxWidth()) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(400.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = {
                    dateRangePickerState.setSelection(null, null)
                    onReset()
                }
            ) { Text("Reset") }
        }
    }

    LaunchedEffect(
        dateRangePickerState.selectedStartDateMillis,
        dateRangePickerState.selectedEndDateMillis
    ) {
        val newStartDate =
            dateRangePickerState.selectedStartDateMillis?.let { LocalDate.ofEpochDay(it / 86400000) }
        val newEndDate =
            dateRangePickerState.selectedEndDateMillis?.let { LocalDate.ofEpochDay(it / 86400000) }

        if (newStartDate != startDate || newEndDate != endDate) {
            onDateRangeSelected(Pair(newStartDate, newEndDate))
        }
    }
}