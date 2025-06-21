package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ZoomIndicator(
    modifier: Modifier = Modifier,
    visible: Boolean,
    isShowSpeedUp: Boolean,
    zoomText: String,
    isClickable: Boolean,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(visible) }

    LaunchedEffect(visible) {
        if (visible) isVisible = true
        else {
            delay(3000)
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible && zoomText.isNotEmpty() && !isShowSpeedUp,
        modifier = modifier
            .padding(24.dp)
            .background(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (isClickable) Modifier
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                    .clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isClickable) Icon(
                Icons.Default.ZoomIn,
                contentDescription = "Zoom",
                tint = Color.White,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = zoomText,
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}