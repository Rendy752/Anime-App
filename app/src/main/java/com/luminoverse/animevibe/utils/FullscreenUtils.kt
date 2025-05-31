 package com.luminoverse.animevibe.utils

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.Window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object FullscreenUtils {
    fun handleFullscreenToggle(
        window: Window,
        isFullscreen: Boolean,
        isLandscape: Boolean,
        activity: Activity?,
        onFullscreenChange: (Boolean) -> Unit,
        isLockLandscapeOrientation: Boolean = false
    ) {
        val newFullscreenState = !isFullscreen

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.let { controller ->
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (newFullscreenState) {
                controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            }
        }

        activity?.let {
            CoroutineScope(Dispatchers.Main).launch {
                if (newFullscreenState && !isLandscape) {
                    it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                if (!isLockLandscapeOrientation) {
                    delay(3000)
                    it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }
            }
        }

        onFullscreenChange(newFullscreenState)
    }
}
