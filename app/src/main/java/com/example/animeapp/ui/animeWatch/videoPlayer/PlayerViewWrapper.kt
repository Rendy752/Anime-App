package com.example.animeapp.ui.animeWatch.videoPlayer

import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R as RMedia3

@OptIn(UnstableApi::class)
@Composable
fun PlayerViewWrapper(
    playerView: PlayerView,
    exoPlayer: ExoPlayer,
    isPipMode: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    isFullscreen: Boolean,
    isLandscape: Boolean,
    onPipVisibilityChange: (Boolean) -> Unit,
    onSpeedChange: (Float, Boolean) -> Unit,
    onHoldingChange: (Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    AndroidView(
        factory = { playerView },
        modifier = Modifier.fillMaxSize()
    ) { view ->
        view.player = exoPlayer
        view.setShowPreviousButton(false)
        view.setShowNextButton(false)
        view.setFullscreenButtonState(true)
        view.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        view.setShowSubtitleButton(true)
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
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isHolding = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isHolding && exoPlayer.playbackParameters.speed != 2f) {
                            exoPlayer.playbackParameters =
                                exoPlayer.playbackParameters.withSpeed(2f)
                            view.useController = false
                            onSpeedChange(2f, true)
                            isFromHolding = true
                        }
                    }, 1000)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
                    if (isFromHolding) {
                        exoPlayer.playbackParameters = exoPlayer.playbackParameters.withSpeed(1f)
                        view.useController = true
                        onSpeedChange(1f, false)
                    }
                    isHolding = false
                    isFromHolding = false
                }
            }
            onHoldingChange(isHolding, isFromHolding)
            if (!isHolding) view.performClick()
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