package com.luminoverse.animevibe.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchView(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = "Search",
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear",
                    )
                }
            }
        },
        label = {
            Text(
                text = placeholder,
                fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
                maxLines = 1
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            focusedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            focusedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unfocusedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            cursorColor = MaterialTheme.colorScheme.onPrimary,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
            unfocusedBorderColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Preview
@Composable
fun SearchViewSkeleton(modifier: Modifier = Modifier) {
    SkeletonBox(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        height = 64.dp
    )
}