package com.luminoverse.animevibe.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ConfirmationAlert(
    title: String,
    message: String,
    confirmText: String = "Quit",
    onConfirm: () -> Unit,
    cancelText: String = "Cancel",
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(message, textAlign = TextAlign.Justify) },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ), onClick = {
                    onCancel()
                    onConfirm()
                }) {
                Text(confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ), onClick = onCancel
            ) {
                Text(cancelText, fontWeight = FontWeight.Bold)
            }
        }
    )
}