package com.luminoverse.animevibe.utils

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object SystemBarsUtils {
    /**
     * Sets the system UI visibility for fullscreen mode.
     *
     * @param activity The activity whose window will be used to control system UI visibility.
     * @param hideSystemBars True to hide system bars for fullscreen, false to show them.
     */
    fun setSystemBarsVisibility(activity: Activity?, hideSystemBars: Boolean) {
        val window = activity?.window ?: return
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (hideSystemBars) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}