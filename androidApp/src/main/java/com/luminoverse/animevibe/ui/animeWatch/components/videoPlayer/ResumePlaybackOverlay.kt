package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
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
import com.luminoverse.animevibe.utils.TimeUtils
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun ResumePlaybackOverlay(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isPipMode: Boolean,
    lastTimestamp: Long,
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
    onResume: (Long) -> Unit
) {
    AnimatedVisibility(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .basicContainer(isPrimary = true, innerPadding = PaddingValues(8.dp))
                .widthIn(min = 192.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.basicContainer(
                        isTertiary = true,
                        onItemClick = onRestart,
                        outerPadding = PaddingValues(0.dp),
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

                Row(
                    modifier = Modifier.basicContainer(
                        isPrimary = true,
                        onItemClick = { onResume(lastTimestamp) },
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
                        text = "Yes, resume",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}