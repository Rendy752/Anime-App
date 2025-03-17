package com.example.animeapp.ui.common_ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun FilterChipView(
    text: String,
    checked: Boolean? = null,
    onCheckedChange: (() -> Unit)? = null
) {
    FilterChip(
        selected = checked == true,
        onClick = {
            if (onCheckedChange != null) onCheckedChange()
        },
        label = { Text(text) },
        leadingIcon = {
            if (checked == true) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Checked"
                )
            }
        },
        border = FilterChipDefaults.filterChipBorder(
            selectedBorderColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            borderColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            enabled = true,
            selected = checked == true,
        ),
    )
}