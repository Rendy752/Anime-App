package com.example.animeapp.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.animeapp.utils.TimeUtils
import com.example.animeapp.utils.basicContainer

@Composable
fun ResumePlaybackOverlay(
    isPipMode: Boolean,
    lastTimestamp: Long,
    onClose: () -> Unit,
    onRestart: () -> Unit,
    onResume: (Long) -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier.basicContainer(isPrimary = true, innerPadding = PaddingValues(8.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = if (!isPipMode) 16.dp else 0.dp)
        ) {
            Text(
                buildAnnotatedString {
                    append("Resume from ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(TimeUtils.formatTimestamp(lastTimestamp))
                    pop()
                    append(" ?")
                },
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.basicContainer(
                        isTertiary = true,
                        onItemClick = onRestart,
                        innerPadding = PaddingValues(8.dp)
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = "Restart",
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                    if (!isPipMode) Text(
                        text = "No, restart",
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(
                    modifier = Modifier.basicContainer(
                        isPrimary = true,
                        onItemClick = { onResume(lastTimestamp) },
                        innerPadding = PaddingValues(8.dp)
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Resume",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    if (!isPipMode) Text(
                        text = "Yes, resume",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        if (!isPipMode) IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}