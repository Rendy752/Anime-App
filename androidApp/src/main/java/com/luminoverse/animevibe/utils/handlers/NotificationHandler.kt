package com.luminoverse.animevibe.utils.handlers

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
import com.luminoverse.animevibe.android.R
import com.luminoverse.animevibe.models.Notification
import com.luminoverse.animevibe.utils.receivers.NotificationReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import javax.inject.Inject

class NotificationHandler @Inject constructor() {

    companion object {
        private const val CHANNEL_ID = "anime_notifications"
        private const val CHANNEL_NAME = "Anime Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for anime events"
    }

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
            setBypassDnd(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        log(
            "Channel created: $CHANNEL_ID, importance=${
                notificationManager.getNotificationChannel(
                    CHANNEL_ID
                )?.importance
            }, lockscreenVisibility=${channel.lockscreenVisibility}"
        )
    }

    suspend fun sendNotification(
        context: Context,
        notification: Notification,
        notificationId: Int
    ) = withContext(Dispatchers.IO) {
        createNotificationChannel(context)
        val actions = when (notification.type) {
            "Broadcast" -> listOf(
                actionDetail(notification.accessId),
                actionClose(notificationId)
            )

            "UnfinishedWatch" -> listOf(
                actionWatch(notification.accessId),
                actionClose(notificationId)
            )

            else -> {
                log("Invalid notification type: ${notification.type} for accessId: ${notification.accessId}")
                emptyList()
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_active_black_24dp)
            .setContentText(notification.contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createOpenIntent(context, notification.type, notification.accessId))
            .setAutoCancel(true)
            .setShowWhen(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .applyImage(context, notification.type, notification.imageUrl)
            .applyActions(context, actions)

        when (notification.type) {
            "Broadcast" -> builder.setContentTitle("Anime Airing Soon")
            "UnfinishedWatch" -> builder.setContentTitle("Unfinished Anime")
            else -> log("Skipping image handling for invalid type: ${notification.type}")
        }

        try {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.notify(notificationId, builder.build())
            log("Notification sent for ${notification.contentText} (accessId: ${notification.accessId}, id: $notificationId, type: ${notification.type})")
        } catch (e: Exception) {
            log("Failed to send notification: ${e.message}, accessId: ${notification.accessId}, type: ${notification.type}, stacktrace=${e.stackTraceToString()}")
        }
    }

    private fun createOpenIntent(context: Context, type: String, accessId: String): PendingIntent {
        val uri = when (type) {
            "UnfinishedWatch" -> {
                val parts = accessId.split("||")
                if (parts.size == 2) {
                    val encodedMalId = URLEncoder.encode(parts[0], "UTF-8")
                    val encodedEpisodeId = URLEncoder.encode(parts[1], "UTF-8")
                    "animevibe://anime/watch/$encodedMalId/$encodedEpisodeId".toUri()
                } else {
                    log("Invalid accessId format for UnfinishedWatch: $accessId, falling back to detail")
                    "animevibe://anime/detail/$accessId".toUri()
                }
            }

            else -> "animevibe://anime/detail/$accessId".toUri()
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        return PendingIntent.getActivity(
            context, accessId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionDetail(accessId: String) = NotificationAction(
        icon = R.drawable.ic_open_in_new_black_24dp,
        title = "Detail",
        action = "ACTION_OPEN_DETAIL",
        extraKey = "access_id",
        extraValue = accessId
    )

    private fun actionWatch(accessId: String) = NotificationAction(
        icon = R.drawable.ic_open_in_new_black_24dp,
        title = "Watch Now",
        action = "ACTION_OPEN_EPISODE",
        extraKey = "access_id",
        extraValue = accessId
    )

    private fun actionClose(notificationId: Int) = NotificationAction(
        icon = R.drawable.ic_close_black_24dp,
        title = "Close",
        action = "ACTION_CLOSE_NOTIFICATION",
        extraKey = "notification_id",
        extraValue = notificationId.toString()
    )

    private fun NotificationCompat.Builder.applyActions(
        context: Context,
        actions: List<NotificationAction>
    ): NotificationCompat.Builder = apply {
        actions.forEach { action ->
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                this.action = action.action
                putExtra(action.extraKey, action.extraValue)
            }
            val pendingIntent = when (action.action) {
                "ACTION_OPEN_DETAIL" -> {
                    val detailIntent = Intent(
                        Intent.ACTION_VIEW,
                        "animevibe://anime/detail/${action.extraValue}".toUri()
                    )
                    PendingIntent.getActivity(
                        context, action.extraValue.hashCode(), detailIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                "ACTION_OPEN_EPISODE" -> {
                    val parts = action.extraValue.split("||")
                    if (parts.size == 2) {
                        val encodedMalId = URLEncoder.encode(parts[0], "UTF-8")
                        val encodedEpisodeId = URLEncoder.encode(parts[1], "UTF-8")
                        val watchIntent = Intent(
                            Intent.ACTION_VIEW,
                            "animevibe://anime/watch/$encodedMalId/$encodedEpisodeId".toUri()
                        )
                        PendingIntent.getActivity(
                            context, action.extraValue.hashCode(), watchIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    } else {
                        log("Invalid accessId format for watch: ${action.extraValue}")
                        null
                    }
                }

                "ACTION_CLOSE_NOTIFICATION" -> {
                    val requestCode =
                        action.extraValue.toIntOrNull() ?: action.extraValue.hashCode()
                    PendingIntent.getBroadcast(
                        context, requestCode, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                else -> PendingIntent.getActivity(
                    context, action.extraValue.hashCode(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            if (pendingIntent != null) {
                addAction(action.icon, action.title, pendingIntent)
            } else {
                log("Skipping action ${action.action} due to null pendingIntent")
            }
        }
    }

    private suspend fun NotificationCompat.Builder.applyImage(
        context: Context,
        type: String,
        imageUrl: String?
    ): NotificationCompat.Builder = apply {
        imageUrl?.let {
            try {
                val request = ImageRequest.Builder(context)
                    .data(it)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                val bitmap = result.drawable?.toBitmap()
                bitmap?.let { bmp ->
                    setLargeIcon(bmp)
                    if (type == "Broadcast") setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bmp)
                            .bigLargeIcon(null as android.graphics.Bitmap?)
                    )
                }
            } catch (e: Exception) {
                log("Error loading image: $imageUrl, error=${e.message}")
            }
        }
    }

    private fun log(message: String) {
        println("NotificationHandler: $message")
    }
}

data class NotificationAction(
    val icon: Int,
    val title: String,
    val action: String,
    val extraKey: String,
    val extraValue: String
)