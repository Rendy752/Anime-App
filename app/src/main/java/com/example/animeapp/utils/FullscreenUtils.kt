package com.example.animeapp.utils

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager

object FullscreenUtils {
    /**
     * Toggles fullscreen mode for the given window, hiding or showing system bars and stabilizing layout.
     * Optionally changes orientation to landscape if entering fullscreen in portrait.
     * @param window The activity window to modify.
     * @param isFullscreen Current fullscreen state.
     * @param isLandscape Current landscape orientation state.
     * @param activity The activity to modify orientation (optional).
     * @param onFullscreenChange Callback to update fullscreen state.
     */
    fun handleFullscreenToggle(
        window: Window,
        isFullscreen: Boolean,
        isLandscape: Boolean,
        activity: Activity?,
        onFullscreenChange: (Boolean) -> Unit
    ) {

        val newFullscreenState = !isFullscreen

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.let {
                if (newFullscreenState) {
                    it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                } else {
                    it.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                }
            }
        } else {
            @Suppress("DEPRECATION")
            if (newFullscreenState) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }

        activity?.let {
            if (newFullscreenState && !isLandscape) {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }

        @Suppress("DEPRECATION")
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (newFullscreenState && (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        }

        onFullscreenChange(newFullscreenState)
    }
}