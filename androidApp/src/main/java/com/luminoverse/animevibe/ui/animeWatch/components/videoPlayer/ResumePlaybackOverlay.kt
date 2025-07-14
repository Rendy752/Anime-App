package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.utils.TimeUtils
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun ResumePlaybackOverlay(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isPipMode: Boolean,
    initialSeekPositionMs: Long?,
    lastTimestamp: Long,
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
    onResume: (Long) -> Unit
) {
    val isDeeplinkSeek = initialSeekPositionMs != null && initialSeekPositionMs > 0
    val targetTimestamp = if (isDeeplinkSeek) initialSeekPositionMs else lastTimestamp
    val descriptionText = if (isDeeplinkSeek) "Seek to" else "Resume from"
    val buttonText = if (isDeeplinkSeek) "Yes, seek" else "Yes, resume"

    AnimatedVisibility(
        modifier = modifier.fillMaxSize(),
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .clickable(enabled = false) {}
                    .widthIn(min = 192.dp)
                    .basicContainer(innerPadding = PaddingValues(8.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    buildAnnotatedString {
                        append("$descriptionText ")
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(TimeUtils.formatTimestamp(targetTimestamp))
                        pop()
                        append(" ?")
                    },
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.basicContainer(
                            onItemClick = onRestart,
                            outerPadding = PaddingValues(0.dp),
                            innerPadding = PaddingValues(8.dp)
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.RestartAlt,
                            contentDescription = "Restart",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        if (!isPipMode) Text(
                            text = "No, restart",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.basicContainer(
                            isPrimary = true,
                            onItemClick = { onResume(targetTimestamp) },
                            outerPadding = PaddingValues(0.dp),
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
                            text = buttonText,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}