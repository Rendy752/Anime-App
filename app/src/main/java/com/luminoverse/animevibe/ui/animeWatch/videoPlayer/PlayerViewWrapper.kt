package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView

@androidx.annotation.OptIn(UnstableApi::class)
@SuppressLint("ClickableViewAccessibility")
@Composable
fun PlayerViewWrapper(
    playerView: PlayerView,
    controlsAreVisible: Boolean,
    isFullscreen: Boolean,
    isLandscape: Boolean,
) {
    LaunchedEffect(isLandscape, isLandscape, isFullscreen) {
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        playerView.postDelayed({
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }, 1)
    }

    AndroidView(
        factory = { playerView },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.useController = false
            view.subtitleView?.apply {
                setStyle(
                    CaptionStyleCompat(
                        Color.White.toArgb(),
                        Color.Transparent.toArgb(),
                        Color.Transparent.toArgb(),
                        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                        Color.Black.toArgb(),
                        null
                    )
                )
            }
            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

            view.subtitleView?.setPadding(
                0, 0, 0,
                if (controlsAreVisible && (isLandscape || !isFullscreen)) 100 else 0
            )
        }
    )
}