package com.luminoverse.animevibe.utils.handlers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import com.luminoverse.animevibe.android.R
import com.luminoverse.animevibe.models.Notification
import com.luminoverse.animevibe.utils.receivers.NotificationReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    }

    suspend fun sendNotification(
        context: Context,
        notification: Notification,
        notificationId: Int
    ) = withContext(Dispatchers.IO) {
        createNotificationChannel(context)
        val actions = when (notification.type) {
            "Broadcast" -> listOf(
                actionDetail(notification.accessId, notificationId),
                actionUnfavorite(notification.accessId, notificationId),
                actionClose(notificationId)
            )

            "UnfinishedWatch" -> listOf(
                actionWatch(notification.accessId, notificationId),
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
            .setContentIntent(createOpenIntent(context, notification.type, notification.accessId, notificationId))
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
            log("Failed to send notification: ${e.message}")
        }
    }

    private fun createOpenIntent(context: Context, type: String, accessId: String, notificationId: Int): PendingIntent {
        val action = if (type == "UnfinishedWatch") "ACTION_OPEN_EPISODE" else "ACTION_OPEN_DETAIL"

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = action
            putExtra("access_id", accessId)
            putExtra("notification_id", notificationId.toString())
        }

        val requestCode = (action + accessId + notificationId).hashCode()
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionDetail(accessId: String, notificationId: Int) = NotificationAction(
        icon = R.drawable.ic_open_in_new_black_24dp,
        title = "Detail",
        action = "ACTION_OPEN_DETAIL",
        extras = mapOf("access_id" to accessId, "notification_id" to notificationId.toString())
    )

    private fun actionUnfavorite(accessId: String, notificationId: Int) = NotificationAction(
        icon = R.drawable.ic_favorite_black_24dp,
        title = "Unfavorite",
        action = "ACTION_UNFAVORITE_ANIME",
        extras = mapOf("access_id" to accessId, "notification_id" to notificationId.toString())
    )

    private fun actionWatch(accessId: String, notificationId: Int) = NotificationAction(
        icon = R.drawable.ic_open_in_new_black_24dp,
        title = "Watch Now",
        action = "ACTION_OPEN_EPISODE",
        extras = mapOf("access_id" to accessId, "notification_id" to notificationId.toString())
    )

    private fun actionClose(notificationId: Int) = NotificationAction(
        icon = R.drawable.ic_close_black_24dp,
        title = "Close",
        action = "ACTION_CLOSE_NOTIFICATION",
        extras = mapOf("notification_id" to notificationId.toString())
    )

    private fun NotificationCompat.Builder.applyActions(
        context: Context,
        actions: List<NotificationAction>
    ): NotificationCompat.Builder = apply {
        actions.forEach { action ->
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                this.action = action.action
                action.extras.forEach { (key, value) -> putExtra(key, value) }
            }
            val requestCode = (action.action + action.extras.toString()).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            addAction(action.icon, action.title, pendingIntent)
        }
    }

    private suspend fun NotificationCompat.Builder.applyImage(
        context: Context,
        type: String,
        imageUrl: String?
    ): NotificationCompat.Builder = apply {
        imageUrl?.let {
            try {
                val request = ImageRequest.Builder(context).data(it).allowHardware(false).build()
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
        Log.d("NotificationHandler", message)
    }
}

data class NotificationAction(
    val icon: Int,
    val title: String,
    val action: String,
    val extras: Map<String, String> = emptyMap()
)