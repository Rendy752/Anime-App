package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.luminoverse.animevibe.models.Track
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import androidx.media3.ui.R as RMedia3

@SuppressLint("ClickableViewAccessibility")
@OptIn(UnstableApi::class)
@Composable
fun PlayerViewWrapper(
    playerView: PlayerView,
    mediaController: MediaControllerCompat?,
    tracks: List<Track>,
    isFullscreen: Boolean,
    isLandscape: Boolean,
    isLocked: Boolean,
    onPlayPause: () -> Unit,
    onPipVisibilityChange: (Boolean) -> Unit,
    onSpeedChange: (Float, Boolean) -> Unit,
    onHoldingChange: (Boolean, Boolean) -> Unit,
    onSeek: (Int, Long) -> Unit,
    onFastForward: () -> Unit,
    onRewind: () -> Unit,
    onControlsToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isSeeking by remember { mutableStateOf(false) }
    val handler = remember { Handler(Looper.getMainLooper()) }

    LaunchedEffect(isLandscape, isFullscreen) {
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        playerView.postDelayed({
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }, 1)
    }

    AndroidView(
        factory = { playerView },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.setShowPreviousButton(false)
            view.setShowNextButton(false)
            view.setShowRewindButton(false)
            view.setShowFastForwardButton(false)
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
            view.setShowSubtitleButton(tracks.any { it.kind == "captions" })
            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

            var isHolding = false
            var isFromHolding = false

            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (!isSeeking && !isLocked && mediaController != null) {
                        Log.d("PlayerViewWrapper", "Double tap detected at x=${e.x}")
                        handler.removeCallbacksAndMessages(null)
                        if (isFromHolding) {
                            Log.d("PlayerViewWrapper", "Resetting speed from holding")
                            HlsPlayerUtils.dispatch(HlsPlayerAction.SetPlaybackSpeed(1f))
                            onSpeedChange(1f, false)
                            isHolding = false
                            isFromHolding = false
                            onHoldingChange(false, false)
                        }

                        val screenWidth = view.width
                        val tapX = e.x

                        when {
                            tapX < screenWidth * 0.4 -> {
                                Log.d("PlayerViewWrapper", "Rewind triggered")
                                onRewind()
                                onSeek(-1, 10L)
                                isSeeking = true
                                onControlsToggle(true)
                            }
                            tapX > screenWidth * 0.6 -> {
                                Log.d("PlayerViewWrapper", "Fast forward triggered")
                                onFastForward()
                                onSeek(1, 10L)
                                isSeeking = true
                                onControlsToggle(true)
                            }
                            else -> {
                                Log.d("PlayerViewWrapper", "Play/Pause triggered")
                                onPlayPause()
                                isSeeking = true
                                onControlsToggle(true)
                            }
                        }
                        handler.postDelayed({
                            isSeeking = false
                            Log.d("PlayerViewWrapper", "Double tap seek reset: isSeeking=false")
                        }, 500)
                        return true
                    }
                    Log.d("PlayerViewWrapper", "Double tap blocked: isSeeking=$isSeeking, isLocked=$isLocked, mediaController=$mediaController")
                    return false
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (!isLocked) {
                        Log.d("PlayerViewWrapper", "Single tap confirmed, isSeeking=$isSeeking")
                        onControlsToggle(!HlsPlayerUtils.state.value.isControlsVisible)
                        return true
                    }
                    return false
                }
            })

            view.setOnTouchListener { _, event ->
                if (playerView.player == null || isLocked) {
                    Log.d("PlayerViewWrapper", "Touch ignored: player=${playerView.player}, isLocked=$isLocked")
                    return@setOnTouchListener false
                }
                gestureDetector.onTouchEvent(event)
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!isSeeking) {
                            Log.d("PlayerViewWrapper", "ACTION_DOWN: isHolding=true")
                            isHolding = true
                            handler.removeCallbacksAndMessages(null)
                            handler.postDelayed({
                                if (isHolding && mediaController?.playbackState?.playbackSpeed != 2f && !isSeeking && mediaController?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
                                    Log.d("PlayerViewWrapper", "Long press: Setting speed to 2f")
                                    HlsPlayerUtils.dispatch(HlsPlayerAction.SetPlaybackSpeed(2f))
                                    onSpeedChange(2f, true)
                                    isFromHolding = true
                                    onControlsToggle(true)
                                } else {
                                    Log.d("PlayerViewWrapper", "Long press not triggered: isHolding=$isHolding, speed=${mediaController?.playbackState?.playbackSpeed}, isSeeking=$isSeeking, state=${mediaController?.playbackState?.state}")
                                }
                            }, 1000)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        Log.d("PlayerViewWrapper", "ACTION_UP/CANCEL: Resetting states")
                        handler.removeCallbacksAndMessages(null)
                        if (isFromHolding && mediaController != null) {
                            Log.d("PlayerViewWrapper", "Resetting speed to 1f")
                            HlsPlayerUtils.dispatch(HlsPlayerAction.SetPlaybackSpeed(1f))
                            onSpeedChange(1f, false)
                        }
                        if (isHolding || isFromHolding) {
                            isHolding = false
                            isFromHolding = false
                            onHoldingChange(false, false)
                        }
                    }
                }
                true
            }

            view.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                if (!isLocked) {
                    Log.d("PlayerViewWrapper", "Controller visibility changed: $visibility")
                    onPipVisibilityChange(visibility == View.VISIBLE)
                    view.subtitleView?.let { subtitleView ->
                        val bottomBar = view.findViewById<ViewGroup>(RMedia3.id.exo_bottom_bar)
                        subtitleView.setPadding(
                            0, 0, 0,
                            if (visibility == View.VISIBLE && (isLandscape || !isFullscreen)) bottomBar.height else 0
                        )
                    }
                }
            })
        }
    )
}