package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SeekIndicator(
    seekDirection: Int,
    seekAmount: Long,
    isLandscape: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val horizontalBias = when (seekDirection) {
        1 -> if (isLandscape) 0.5f else 0.75f
        -1 -> -if (isLandscape) 0.5f else 0.75f
        else -> 0f
    }

    AnimatedVisibility(
        visible = seekAmount != 0L && seekDirection != 0 && errorMessage == null,
        enter = scaleIn(animationSpec = tween(durationMillis = 300)) + fadeIn(
            animationSpec = tween(
                durationMillis = 300
            )
        ),
        exit = scaleOut(animationSpec = tween(durationMillis = 300)) + fadeOut(
            animationSpec = tween(
                durationMillis = 300
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .align(BiasAlignment(horizontalBias = horizontalBias, verticalBias = 0f))
                    .background(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (seekDirection == 1) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Fast forward",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (seekDirection == -1) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (seekAmount != 0L) {
                    Text(
                        text = "${if (seekDirection == 1) "" else "-"}${seekAmount} seconds",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}