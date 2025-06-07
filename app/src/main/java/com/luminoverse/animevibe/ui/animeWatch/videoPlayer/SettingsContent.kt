package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.Track
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun SettingsContent(
    onDismiss: () -> Unit,
    onLockClick: () -> Unit,
    selectedPlaybackSpeed: Float,
    onPlaybackSpeedClick: () -> Unit,
    isSubtitleAvailable: Boolean,
    selectedSubtitle: Track?,
    onSubtitleClick: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .basicContainer(
                        roundedCornerShape = RoundedCornerShape(0.dp),
                        outerPadding = PaddingValues(0.dp),
                        innerPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        onItemClick = { onLockClick(); onDismiss() }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Lock Screen",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .basicContainer(
                        roundedCornerShape = RoundedCornerShape(0.dp),
                        outerPadding = PaddingValues(0.dp),
                        innerPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        onItemClick = { onPlaybackSpeedClick();onDismiss() }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Playback Speed Icon",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedPlaybackSpeed}x",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Forward Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .basicContainer(
                        roundedCornerShape = RoundedCornerShape(0.dp),
                        outerPadding = PaddingValues(0.dp),
                        innerPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        onItemClick = if (isSubtitleAvailable) {
                            { onSubtitleClick(); onDismiss() }
                        } else {{}}
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Subtitle Icon",
                        tint = if (isSubtitleAvailable) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                    Text(
                        text = "Subtitles",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSubtitleAvailable) {
                            MaterialTheme.colorScheme.onSurface
                        } else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedSubtitle?.label ?: "Unavailable",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isSubtitleAvailable) {
                            MaterialTheme.colorScheme.primary
                        } else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Forward Icon",
                        tint = if (isSubtitleAvailable) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        }
    }
}