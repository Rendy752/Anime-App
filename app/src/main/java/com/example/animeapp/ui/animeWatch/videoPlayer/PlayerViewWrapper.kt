package com.example.animeapp.ui.animeWatch.videoPlayer

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.animeapp.models.Track
import com.example.animeapp.utils.FullscreenUtils
import com.example.animeapp.utils.HlsPlayerUtil
import com.example.animeapp.utils.PlayerAction
import androidx.media3.ui.R as RMedia3

@SuppressLint("ClickableViewAccessibility")
@OptIn(UnstableApi::class)
@Composable
fun PlayerViewWrapper(
    playerView: PlayerView,
    mediaController: MediaControllerCompat?,
    tracks: List<Track>,
    isPipMode: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    isFullscreen: Boolean,
    isLandscape: Boolean,
    isLocked: Boolean,
    onPipVisibilityChange: (Boolean) -> Unit,
    onSpeedChange: (Float, Boolean) -> Unit,
    onHoldingChange: (Boolean, Boolean) -> Unit,
    onSeek: (Int, Long) -> Unit,
    onFastForward: () -> Unit,
    onRewind: () -> Unit,
) {
    val context = LocalContext.current
    var isSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(isSeeking, isLocked) {
        if (!isSeeking || isLocked) playerView.hideController()
    }

    AndroidView(
        factory = { playerView },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.setShowPreviousButton(false)
            view.setShowNextButton(false)
            view.setShowRewindButton(false)
            view.setShowFastForwardButton(false)
            view.setShowSubtitleButton(tracks.any { it.kind == "captions" })
            view.useController = !isPipMode && !isLocked
            playerView.controllerShowTimeoutMs = if (isPipMode || isLocked) 0 else 5000
            view.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)

            view.resizeMode = if (!isLandscape && isFullscreen) {
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            } else {
                AspectRatioFrameLayout.RESIZE_MODE_FILL
            }

            view.fitsSystemWindows = !isFullscreen
            if (isFullscreen) {
                view.setPadding(0, 0, 0, 0)
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            view.setOnApplyWindowInsetsListener { v, insets ->
                if (isFullscreen) {
                    v.setPadding(0, 0, 0, 0)
                    insets
                } else {
                    v.onApplyWindowInsets(insets)
                }
            }

            view.setFullscreenButtonClickListener {
                if (!isLocked) {
                    (context as? FragmentActivity)?.window?.let { window ->
                        FullscreenUtils.handleFullscreenToggle(window, isFullscreen, onFullscreenChange)
                    }
                }
            }

            var isHolding = false
            var isFromHolding = false

            val gestureDetector =
                GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        if (!isSeeking && !isLocked && mediaController != null) {
                            val screenWidth = view.width
                            val tapX = e.x

                            if (tapX > screenWidth / 2) {
                                onFastForward()
                                onSeek(1, 10L)
                            } else {
                                onRewind()
                                onSeek(-1, 10L)
                            }
                            isSeeking = true
                            view.showController()
                            Handler(Looper.getMainLooper()).postDelayed({
                                isSeeking = false
                            }, 1000)
                            return true
                        }
                        return false
                    }

                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        if (!isSeeking && !isLocked) {
                            view.performClick()
                            return true
                        }
                        return false
                    }
                })

            view.setOnTouchListener { _, event ->
                if (playerView.player == null || isLocked) return@setOnTouchListener false
                gestureDetector.onTouchEvent(event)
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isHolding = true
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isHolding && mediaController?.playbackState?.playbackSpeed != 2f && !isSeeking && mediaController?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
                                HlsPlayerUtil.dispatch(PlayerAction.SetPlaybackSpeed(2f))
                                view.useController = false
                                onSpeedChange(2f, true)
                                isFromHolding = true
                            }
                        }, 1000)
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
                        if (isFromHolding && mediaController != null) {
                            HlsPlayerUtil.dispatch(PlayerAction.SetPlaybackSpeed(1f))
                            view.useController = true
                            onSpeedChange(1f, false)
                        }
                        isHolding = false
                        isFromHolding = false
                    }
                }
                onHoldingChange(isHolding, isFromHolding)
                true
            }

            view.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                if (!isLocked) {
                    onPipVisibilityChange(visibility == View.VISIBLE)
                    view.subtitleView?.let { subtitleView ->
                        val bottomBar = view.findViewById<ViewGroup>(RMedia3.id.exo_bottom_bar)
                        subtitleView.setPadding(
                            0, 0, 0,
                            if (visibility == View.VISIBLE && (isLandscape || (context as? FragmentActivity)?.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT)) bottomBar.height else 0
                        )
                    }
                }
            })
        }
    )
}