package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.round

@Composable
fun PlaybackSpeedContent(
    selectedPlaybackSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val speedRange = 0.25f..2.0f
    val steps = 34
    val presets = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(selectedPlaybackSpeed) {
        val targetIndex = presets.indexOfFirst { it == selectedPlaybackSpeed }
        if (targetIndex != -1) {
            scope.launch {
                val buttonWidthPx = with(density) { 90.dp.toPx() }
                val targetScrollPosition =
                    (buttonWidthPx * targetIndex) - (scrollState.maxValue / 2)

                scrollState.animateScrollTo(
                    targetScrollPosition.toInt().coerceIn(0, scrollState.maxValue)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = String.format(Locale.US, "%.2fx", selectedPlaybackSpeed),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable {
                        val newSpeed = (selectedPlaybackSpeed - 0.05f).coerceIn(speedRange)
                        onSpeedChange(round(newSpeed * 100) / 100f)
                    }
                    .padding(8.dp),
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease Speed",
                tint = MaterialTheme.colorScheme.onSurface
            )

            Slider(
                value = selectedPlaybackSpeed,
                onValueChange = { onSpeedChange(round(it * 100) / 100f) },
                modifier = Modifier.weight(1f),
                valueRange = speedRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Icon(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable {
                        val newSpeed = (selectedPlaybackSpeed + 0.05f).coerceIn(speedRange)
                        onSpeedChange(round(newSpeed * 100) / 100f)
                    }
                    .padding(8.dp),
                imageVector = Icons.Default.Add,
                contentDescription = "Increase Speed",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            presets.forEach { speed ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FilledTonalButton(
                        onClick = { onSpeedChange(speed) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (selectedPlaybackSpeed == speed) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            contentColor = if (selectedPlaybackSpeed == speed) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    ) {
                        Text(text = if (speed == 1.0f) "1.0" else speed.toString())
                    }
                    if (speed == 1.0f) {
                        Text(
                            text = "Normal",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}