package com.luminoverse.animevibe.ui.animeSearch.components

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun CancelButton(
    cancelAction: () -> Unit,
    modifier: Modifier? = Modifier
) {
    Button(
        modifier = modifier ?: Modifier,
        onClick = cancelAction,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(
            text = "Cancel", maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ResetButton(
    isDefault: () -> Boolean,
    resetAction: () -> Unit,
    modifier: Modifier? = Modifier
) {
    Button(
        modifier = modifier ?: Modifier,
        onClick = resetAction,
        enabled = !isDefault(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Text(
            text = "Reset", maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ApplyButton(
    isEmptySelection: () -> Boolean,
    applyAction: () -> Unit,
    modifier: Modifier? = Modifier
) {
    Button(
        modifier = modifier ?: Modifier,
        onClick = applyAction,
        enabled = !isEmptySelection(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = "Apply", maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}