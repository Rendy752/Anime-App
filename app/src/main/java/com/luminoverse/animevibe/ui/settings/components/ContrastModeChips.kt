package com.luminoverse.animevibe.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminoverse.animevibe.ui.theme.ContrastMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContrastModeChips(
    selectedContrastMode: ContrastMode,
    onContrastModeChanged: (ContrastMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Contrast Mode",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Adjust the contrast level for better visibility",
                fontSize = 12.sp
            )
        }
        FilterChip(
            selected = true,
            onClick = {
                val contrastModeOptions = ContrastMode.entries
                val currentIndex = contrastModeOptions.indexOf(selectedContrastMode)
                val nextIndex = (currentIndex + 1) % contrastModeOptions.size
                onContrastModeChanged(contrastModeOptions[nextIndex])
            },
            label = { Text(selectedContrastMode.name) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}