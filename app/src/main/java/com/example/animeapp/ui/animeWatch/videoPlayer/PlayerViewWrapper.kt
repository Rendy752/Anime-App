package com.example.animeapp.ui.animeWatch.videoPlayer

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
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
import androidx.media3.ui.PlayerView
import com.example.animeapp.models.Track
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
    onPipVisibilityChange: (Boolean) -> Unit,
    onSpeedChange: (Float, Boolean) -> Unit,
    onHoldingChange: (Boolean, Boolean) -> Unit,
    onSeek: (Int, Long) -> Unit,
    onFastForward: () -> Unit,
    onRewind: () -> Unit,
) {
    val context = LocalContext.current
    var isSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(isSeeking) {
        if (!isSeeking) playerView.hideController()
    }

    AndroidView(
        factory = { playerView },
        modifier = Modifier.fillMaxSize()
    ) { view ->
        view.setShowPreviousButton(false)
        view.setShowNextButton(false)
        view.setShowRewindButton(false)
        view.setShowFastForwardButton(false)
        view.setFullscreenButtonState(true)
        view.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        view.setShowSubtitleButton(tracks.any { it.kind == "captions" })
        view.useController = !isPipMode

        view.setFullscreenButtonClickListener {
            onFullscreenChange(!isFullscreen)
            (context as? FragmentActivity)?.window?.let { window ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val controller = window.insetsController
                    if (!isFullscreen) {
                        controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        controller?.systemBarsBehavior =
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        controller?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    }
                } else {
                    if (!isFullscreen) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    }
                }
            }
        }

        var isHolding = false
        var isFromHolding = false

        val gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (!isSeeking && mediaController != null) {
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
                    if (!isSeeking) {
                        view.performClick()
                        return true
                    }
                    return false
                }
            })

        view.setOnTouchListener { _, event ->
            if (playerView.player == null) return@setOnTouchListener false
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isHolding = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isHolding && mediaController?.playbackState?.playbackSpeed != 2f && !isSeeking && mediaController?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
                            mediaController.transportControls?.setPlaybackSpeed(2f)
                            view.useController = false
                            onSpeedChange(2f, true)
                            isFromHolding = true
                        }
                    }, 1000)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
                    if (isFromHolding && mediaController != null) {
                        mediaController.transportControls?.setPlaybackSpeed(1f)
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
            onPipVisibilityChange(visibility == View.VISIBLE)
            view.subtitleView?.let { subtitleView ->
                val bottomBar = view.findViewById<ViewGroup>(RMedia3.id.exo_bottom_bar)
                subtitleView.setPadding(
                    0, 0, 0,
                    if (visibility == View.VISIBLE && (isLandscape || (context as? FragmentActivity)?.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT)) bottomBar.height else 0
                )
            }
        })
    }
}