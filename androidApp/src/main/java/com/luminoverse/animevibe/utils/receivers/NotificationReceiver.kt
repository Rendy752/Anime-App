package com.luminoverse.animevibe.utils.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ACTION_CLOSE_NOTIFICATION") {
            val notificationId = intent.getIntExtra("notification_id", -1)

            if (notificationId != -1) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
                println("NotificationReceiver: Closed notification $notificationId")
            } else {
                println("NotificationReceiver: Invalid notification_id for close action")
            }
        }
    }
}