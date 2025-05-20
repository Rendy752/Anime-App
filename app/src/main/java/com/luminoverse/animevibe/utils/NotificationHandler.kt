package com.luminoverse.animevibe.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import coil.imageLoader
import coil.request.ImageRequest
import com.luminoverse.animevibe.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class NotificationType {
    data class Broadcast(
        val malId: Int,
        val title: String,
        val imageUrl: String?
    ) : NotificationType()

    data class UnfinishedAnime(
        val malId: Int,
        val title: String,
        val episode: Int,
        val imageUrl: String?
    ) : NotificationType()
}

data class NotificationData(
    val malId: Int,
    val title: String,
    val imageUrl: String?,
    val contentText: String,
    val actions: List<NotificationAction>
)

class NotificationHandler @javax.inject.Inject constructor() {

    companion object {
        const val CHANNEL_ID = "anime_notifications"
    }

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Anime Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Notifications for anime events" }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
        println("NotificationHandler: Channel created: $CHANNEL_ID")
    }

    suspend fun sendNotification(context: Context, type: NotificationType) = withContext(Dispatchers.IO) {
        createNotificationChannel(context)

        val notificationData = when (type) {
            is NotificationType.Broadcast -> NotificationData(
                malId = type.malId,
                title = type.title,
                imageUrl = type.imageUrl,
                contentText = "${type.title} is about to air!",
                actions = listOf(
                    NotificationAction(
                        icon = R.drawable.ic_open_in_new_black_24dp,
                        title = "Detail",
                        action = "ACTION_OPEN_DETAIL",
                        extraKey = "mal_id",
                        extraValue = type.malId
                    ),
                    NotificationAction(
                        icon = R.drawable.ic_close_black_24dp,
                        title = "Close",
                        action = "ACTION_CLOSE_NOTIFICATION",
                        extraKey = "notification_id",
                        extraValue = type.malId
                    )
                )
            )
            is NotificationType.UnfinishedAnime -> NotificationData(
                malId = type.malId,
                title = type.title,
                imageUrl = type.imageUrl,
                contentText = "Continue watching ${type.title}, Episode ${type.episode}!",
                actions = listOf(
                    NotificationAction(
                        icon = R.drawable.ic_open_in_new_black_24dp,
                        title = "Watch Now",
                        action = "ACTION_OPEN_EPISODE",
                        extraKey = "mal_id",
                        extraValue = type.malId
                    ),
                    NotificationAction(
                        icon = R.drawable.ic_close_black_24dp,
                        title = "Close",
                        action = "ACTION_CLOSE_NOTIFICATION",
                        extraKey = "notification_id",
                        extraValue = type.malId
                    )
                )
            )
        }

        with(notificationData) {
            val openIntent = Intent(Intent.ACTION_VIEW, "animevibe://anime/detail/$malId".toUri())
            val openPendingIntent = PendingIntent.getActivity(
                context,
                malId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_active_black_24dp)
                .setContentTitle("Anime Notification")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)

            actions.forEach { action ->
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    this.action = action.action
                    putExtra(action.extraKey, action.extraValue)
                }
                val pendingIntent = when (action.action) {
                    "ACTION_CLOSE_NOTIFICATION" -> PendingIntent.getBroadcast(
                        context,
                        malId + 1,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    else -> PendingIntent.getActivity(
                        context,
                        malId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                builder.addAction(action.icon, action.title, pendingIntent)
            }

            if (imageUrl != null) {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .allowHardware(false)
                        .build()
                    val result = context.imageLoader.execute(request)
                    val bitmap = result.drawable?.toBitmap()
                    if (bitmap != null) {
                        builder.setLargeIcon(bitmap)
                            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                    }
                } catch (e: Exception) {
                    println("NotificationHandler: Error loading image: $imageUrl, error=${e.message}")
                }
            }

            context.getSystemService(NotificationManager::class.java).notify(malId, builder.build())
            println("NotificationHandler: Notification sent for $title (malId: $malId)")
        }
    }
}

data class NotificationAction(
    val icon: Int,
    val title: String,
    val action: String,
    val extraKey: String,
    val extraValue: Int
)