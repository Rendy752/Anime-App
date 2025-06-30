package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminoverse.animevibe.models.TimeRange

@Composable
fun SkipButtonsContainer(
    modifier: Modifier = Modifier,
    currentPosition: Long,
    duration: Long,
    intro: TimeRange,
    outro: TimeRange,
    isSkipVisible: Boolean,
    onSkip: (Long) -> Unit
) {
    val showIntroButton =
        currentPosition >= intro.start * 1000L && currentPosition < intro.end * 1000L && currentPosition in 0 until duration

    val showOutroButton =
        currentPosition >= outro.start * 1000L && currentPosition < outro.end * 1000L && currentPosition in 0 until duration

    SkipButton(
        label = "Skip Intro",
        isVisible = showIntroButton && isSkipVisible,
        onSkip = { onSkip(intro.end * 1000L) },
        modifier = modifier
    )
    SkipButton(
        label = "Skip Outro",
        isVisible = showOutroButton && isSkipVisible,
        onSkip = { onSkip(outro.end * 1000L) },
        modifier = modifier
    )
}

@Composable
private fun SkipButton(
    label: String,
    isVisible: Boolean,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier
            .padding(end = 56.dp, bottom = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Color.White, RoundedCornerShape(16.dp))
            .clickable { onSkip() }
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Zoom",
                tint = Color.White,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}