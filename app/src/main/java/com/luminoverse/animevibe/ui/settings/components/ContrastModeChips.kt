package com.luminoverse.animevibe.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            Text(
                text = "Contrast Mode",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Adjust the contrast level for better visibility",
                fontSize = 12.sp
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContrastMode.entries.forEach { mode ->
                FilterChip(
                    selected = selectedContrastMode == mode,
                    onClick = { onContrastModeChanged(mode) },
                    label = { Text(mode.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "Contrast mode ${mode.name}"
                    }
                )
            }
        }
    }
}