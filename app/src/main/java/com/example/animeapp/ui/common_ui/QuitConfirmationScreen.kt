package com.example.animeapp.ui.common_ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun QuitConfirmationAlert(
    onDismissRequest: () -> Unit,
    onQuitConfirmed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Quit AnimeApp?") },
        text = { Text("Are you sure you want to quit the app?") },
        confirmButton = {
            Button(onClick = {
                onDismissRequest()
                onQuitConfirmed()
            }) {
                Text("Quit")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}