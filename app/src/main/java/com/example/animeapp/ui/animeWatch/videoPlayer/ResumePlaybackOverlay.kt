package com.example.animeapp.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.example.animeapp.utils.TimeUtils
import com.example.animeapp.utils.basicContainer

@Composable
fun ResumePlaybackOverlay(
    lastTimestamp: Long,
    onClose: () -> Unit,
    onRestart: () -> Unit,
    onResume: (Long) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.basicContainer(isPrimary = true),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        Text(
            buildAnnotatedString {
                append("Resume from ")
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(TimeUtils.formatTimestamp(lastTimestamp))
                pop()
                append(" ?")
            },
            color = MaterialTheme.colorScheme.onPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.basicContainer(isTertiary = true, onItemClick = onRestart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.RestartAlt,
                    contentDescription = "Restart",
                    tint = MaterialTheme.colorScheme.onTertiary
                )
                Text(
                    text = "No, restart",
                    color = MaterialTheme.colorScheme.onTertiary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.basicContainer(
                    isPrimary = true,
                    onItemClick = { onResume(lastTimestamp) }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Resume",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Yes, resume",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}