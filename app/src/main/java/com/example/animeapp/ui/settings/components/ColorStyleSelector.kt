package com.example.animeapp.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.example.animeapp.ui.theme.ColorStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorStyleSelector(
    colorStyle: ColorStyle,
    onColorStyleChanged: (ColorStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Color Style")
            Text(
                text = "Choose the app's color scheme",
                fontSize = 12.sp
            )
        }
        ExposedDropdownMenuBox(
            expanded = isMenuExpanded,
            onExpandedChange = { isMenuExpanded = it }
        ) {
            TextField(
                value = colorStyle.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMenuExpanded) },
                modifier = Modifier
                    .menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                        enabled = true
                    )
                    .semantics { contentDescription = "Color style selector" }
            )
            ExposedDropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                ColorStyle.entries.forEach { style ->
                    DropdownMenuItem(
                        text = { Text(style.name) },
                        onClick = {
                            onColorStyleChanged(style)
                            isMenuExpanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}