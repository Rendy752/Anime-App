package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.Track
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun SettingsContent(
    onDismiss: () -> Unit,
    onLockClick: () -> Unit,
    onPipClick: () -> Unit,
    selectedPlaybackSpeed: Float,
    onPlaybackSpeedClick: () -> Unit,
    isSubtitleAvailable: Boolean,
    selectedSubtitle: Track?,
    onSubtitleClick: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            SettingsContentItem(
                icon = Icons.Default.Lock,
                name = "Lock Screen",
                onItemClick = { onLockClick(); onDismiss() }
            )
        }
        item {
            SettingsContentItem(
                icon = Icons.Default.PictureInPictureAlt,
                name = "Enter Picture-in-Picture",
                onItemClick = { onPipClick(); onDismiss() }
            )
        }
        item {
            SettingsContentItem(
                icon = Icons.Default.Speed,
                name = "Playback Speed",
                selectedValue = "${selectedPlaybackSpeed}x",
                onItemClick = { onPlaybackSpeedClick(); onDismiss() }
            )
        }
        item {
            SettingsContentItem(
                icon = Icons.Default.Subtitles,
                name = "Subtitles",
                selectedValue = if (isSubtitleAvailable) selectedSubtitle?.label
                    ?: "None" else "Unavailable",
                isClickable = isSubtitleAvailable,
                onItemClick = if (isSubtitleAvailable) {
                    { onSubtitleClick(); onDismiss() }
                } else null
            )
        }
    }
}

@Composable
private fun SettingsContentItem(
    icon: ImageVector,
    name: String,
    selectedValue: String? = null,
    isClickable: Boolean = true,
    onItemClick: (() -> Unit)?
) {
    val textColor =
        if (isClickable) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val arrowAndSelectedValueColor =
        if (isClickable) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .basicContainer(
                roundedCornerShape = RoundedCornerShape(0.dp),
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                onItemClick = onItemClick
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$name Icon",
                tint = textColor
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }

        selectedValue?.let {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = arrowAndSelectedValueColor,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Forward Icon",
                    tint = arrowAndSelectedValueColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}