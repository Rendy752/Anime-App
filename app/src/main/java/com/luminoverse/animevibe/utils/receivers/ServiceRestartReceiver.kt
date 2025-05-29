package com.luminoverse.animevibe.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.luminoverse.animevibe.AnimeApplication
import com.luminoverse.animevibe.utils.workers.BroadcastNotificationWorker

class ServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == BroadcastNotificationWorker.ACTION_RESTART_SERVICE) {
            (context.applicationContext as AnimeApplication).restartMediaService()
        }
    }
}