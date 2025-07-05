package com.luminoverse.animevibe.utils

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object FullscreenUtils {
    /**
     * Sets the system UI visibility for fullscreen mode.
     *
     * @param window The activity's window. This is used to control the system UI visibility.
     * @param requestHideSystemBars True to hide system bars for fullscreen, false to show them.
     */
    fun setFullscreen(window: Window, requestHideSystemBars: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (requestHideSystemBars) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}