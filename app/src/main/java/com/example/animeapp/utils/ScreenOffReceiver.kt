package com.example.animeapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenOffReceiver(private val onScreenOff: (() -> Unit)? = null) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
            onScreenOff?.invoke()
        }
    }
}