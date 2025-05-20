package com.luminoverse.animevibe.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_CLOSE_NOTIFICATION" -> {
                val notificationId = intent.getIntExtra("notification_id", -1)
                if (notificationId != -1) {
                    context.getSystemService(NotificationManager::class.java)
                        .cancel(notificationId)
                    println("NotificationReceiver: Closed notification $notificationId")
                }
            }
            "ACTION_OPEN_DETAIL", "ACTION_OPEN_EPISODE" -> {
                val malId = intent.getIntExtra("mal_id", -1)
                if (malId != -1) {
                    val openIntent = Intent(Intent.ACTION_VIEW, "animevibe://anime/detail/$malId".toUri())
                    openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(openIntent)
                    println("NotificationReceiver: Opened detail for malId=$malId")
                }
            }
        }
    }
}