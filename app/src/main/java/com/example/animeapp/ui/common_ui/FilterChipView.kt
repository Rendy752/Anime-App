package com.example.animeapp.ui.common_ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun FilterChipView(
    text: String,
    checked: Boolean? = null,
    imageUrl: String? = null,
    onCheckedChange: (() -> Unit)? = null
) {
    FilterChip(
        selected = checked == true,
        onClick = {
            if (onCheckedChange != null) onCheckedChange()
        },
        label = { Text(text) },
        leadingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (checked == true) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Checked"
                    )
                }

                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Image",
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(24.dp)
                    )
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = FilterChipDefaults.filterChipBorder(
            selectedBorderColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            borderColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            enabled = true,
            selected = checked == true,
        ),
    )
}