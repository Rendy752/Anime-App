package com.example.animeapp.ui.settings.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DarkModeToggle(
    isDarkMode: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Dark Mode", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Enable dark mode",
                fontSize = 12.sp
            )
        }

        val animatedScale by animateFloatAsState(
            targetValue = if (isDarkMode) 1.2f else 1f,
            label = "scale"
        )
        Switch(
            checked = isDarkMode,
            modifier = Modifier
                .scale(animatedScale)
                .padding(10.dp)
                .semantics {
                    contentDescription = if (isDarkMode) {
                        "Disable dark mode"
                    } else {
                        "Enable dark mode"
                    }
                },
            onCheckedChange = onDarkModeChanged
        )
    }
}