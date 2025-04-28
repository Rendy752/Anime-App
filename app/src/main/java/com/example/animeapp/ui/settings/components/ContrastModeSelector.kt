package com.example.animeapp.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animeapp.ui.theme.ContrastMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContrastModeSelector(
    contrastMode: ContrastMode,
    onContrastModeChanged: (ContrastMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var isContrastMenuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Contrast Mode", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Adjust the contrast level for better visibility",
                fontSize = 12.sp
            )
        }
        ExposedDropdownMenuBox(
            modifier = Modifier.padding(start = 8.dp),
            expanded = isContrastMenuExpanded,
            onExpandedChange = { isContrastMenuExpanded = it }
        ) {
            TextField(
                value = contrastMode.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isContrastMenuExpanded) },
                modifier = Modifier
                    .menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                        enabled = true
                    )
                    .semantics { contentDescription = "Contrast mode selector" }
            )
            ExposedDropdownMenu(
                expanded = isContrastMenuExpanded,
                onDismissRequest = { isContrastMenuExpanded = false }
            ) {
                ContrastMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.name) },
                        onClick = {
                            onContrastModeChanged(mode)
                            isContrastMenuExpanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}