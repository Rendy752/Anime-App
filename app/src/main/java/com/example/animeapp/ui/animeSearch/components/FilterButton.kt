package com.example.animeapp.ui.animeSearch.components

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.animeapp.R

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
        Text(text = stringResource(id = R.string.cancel))
    }
}

@Composable
fun ResetButton(
    context: Context,
    isDefault: () -> Boolean,
    resetAction: () -> Unit,
    modifier: Modifier? = Modifier
) {
    Button(
        modifier = modifier ?: Modifier,
        onClick = {
            if (isDefault()) {
                Toast.makeText(context, "Filters are already default", Toast.LENGTH_SHORT).show()
            } else {
                resetAction()
            }
        },
        enabled = !isDefault(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Text(stringResource(R.string.reset))
    }
}

@Composable
fun ApplyButton(
    context: Context,
    isEmptySelection: () -> Boolean,
    applyAction: () -> Unit,
    modifier: Modifier? = Modifier
) {
    Button(
        modifier = modifier ?: Modifier,
        onClick = {
            if (isEmptySelection()) {
                Toast.makeText(
                    context,
                    "No filters applied, you can reset",
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else {
                applyAction()
            }
        },
        enabled = !isEmptySelection(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) { Text(stringResource(R.string.apply)) }
}