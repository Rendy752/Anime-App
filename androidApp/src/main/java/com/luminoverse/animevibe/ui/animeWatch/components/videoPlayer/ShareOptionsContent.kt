package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.ui.common.ToggleWithLabel
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun ShareOptionsContent(
    includeTimestamp: Boolean,
    onIncludeTimestampChange: (Boolean) -> Unit,
    generatedLink: String,
    onShareClick: () -> Unit,
    onDismiss: () -> Unit
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ToggleWithLabel(
            isActive = includeTimestamp,
            onToggle = onIncludeTimestampChange,
            label = "Share with Timestamp",
            description = "Includes your current playback position in the link."
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .basicContainer(
                    outerPadding = PaddingValues(0.dp),
                    innerPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = generatedLink,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { clipboardManager.setText(AnnotatedString(generatedLink)) }
                    .padding(8.dp),
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy Link",
                tint = Color.White
            )
        }

        Button(
            onClick = {
                onShareClick()
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Share") }
    }
}