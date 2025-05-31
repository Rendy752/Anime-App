package com.luminoverse.animevibe.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenOnReceiver(private val onScreenOn: (() -> Unit)? = null) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_ON) {
            onScreenOn?.invoke()
        }
    }
}