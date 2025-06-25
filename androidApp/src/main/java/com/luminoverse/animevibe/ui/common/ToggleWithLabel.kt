package com.luminoverse.animevibe.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToggleWithLabel(
    isActive: Boolean,
    label: String,
    description: String,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                fontSize = 12.sp
            )
        }

        val animatedScale by animateFloatAsState(
            targetValue = if (isActive) 1.1f else 1f,
            label = "scale"
        )
        Switch(
            checked = isActive,
            modifier = Modifier.scale(animatedScale),
            onCheckedChange = onToggle
        )
    }
}