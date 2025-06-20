package com.luminoverse.animevibe.utils

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object FullscreenUtils {
    /**
     * Handles showing or hiding the system bars (status and navigation).
     * The actual orientation change is handled as a side effect in the Composable.
     */
    fun handleFullscreenToggle(
        window: Window,
        isFullscreen: Boolean,
        setFullscreenChange: (Boolean) -> Unit
    ) {
        val newFullscreenState = !isFullscreen
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.let { controller ->
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (newFullscreenState) {
                controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            }
        }

        setFullscreenChange(newFullscreenState)
    }
}