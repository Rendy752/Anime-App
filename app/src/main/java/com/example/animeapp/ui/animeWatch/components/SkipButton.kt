package com.example.animeapp.ui.animeWatch.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SkipButton(
    label: String,
    skipTime: Long,
    onSkip: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onSkip(skipTime) },
        modifier = modifier.padding(end = 80.dp, bottom = 80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(label)
    }
}