package com.luminoverse.animevibe.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ACTION_CLOSE_NOTIFICATION") {
            val notificationId = intent.getIntExtra("notification_id", -1)
            if (notificationId != -1) {
                context.getSystemService(NotificationManager::class.java).cancel(notificationId)
                println("NotificationReceiver: Dismissed notification with ID: $notificationId")
            }
        }
    }
}