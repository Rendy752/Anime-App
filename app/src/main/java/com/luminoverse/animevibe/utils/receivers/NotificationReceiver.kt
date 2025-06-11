package com.luminoverse.animevibe.utils.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import java.net.URLEncoder

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_CLOSE_NOTIFICATION" -> {
                val notificationId = intent.getIntExtra("notification_id", -1)
                if (notificationId != -1) {
                    context.getSystemService(NotificationManager::class.java)
                        .cancel(notificationId)
                    println("NotificationReceiver: Closed notification $notificationId")
                } else {
                    println("NotificationReceiver: Invalid notification_id for close action")
                }
            }
            "ACTION_OPEN_DETAIL" -> {
                val accessId = intent.getStringExtra("access_id")
                if (!accessId.isNullOrEmpty()) {
                    val openIntent = Intent(Intent.ACTION_VIEW, "animevibe://anime/detail/$accessId".toUri())
                    openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    try {
                        context.startActivity(openIntent)
                        println("NotificationReceiver: Opened detail for accessId=$accessId")
                    } catch (e: Exception) {
                        println("NotificationReceiver: Failed to open detail for accessId=$accessId, error=${e.message}")
                    }
                } else {
                    println("NotificationReceiver: Invalid access_id for detail action")
                }
            }
            "ACTION_OPEN_EPISODE" -> {
                val accessId = intent.getStringExtra("access_id")
                if (!accessId.isNullOrEmpty()) {
                    val parts = accessId.split("||")
                    if (parts.size == 2) {
                        val encodedMalId = URLEncoder.encode(parts[0], "UTF-8")
                        val encodedEpisodeId = URLEncoder.encode(parts[1], "UTF-8")
                        val openIntent = Intent(Intent.ACTION_VIEW, "animevibe://anime/watch/$encodedMalId/$encodedEpisodeId".toUri())
                        openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        try {
                            context.startActivity(openIntent)
                            println("NotificationReceiver: Opened episode for malId=$encodedMalId, episodeId=$encodedEpisodeId")
                        } catch (e: Exception) {
                            println("NotificationReceiver: Failed to open episode for malId=$encodedMalId, episodeId=$encodedEpisodeId, error=${e.message}")
                        }
                    } else {
                        println("NotificationReceiver: Invalid accessId format for episode action: $accessId")
                    }
                } else {
                    println("NotificationReceiver: Invalid access_id for episode action")
                }
            }
        }
    }
}