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
        const val BROADCAST_CHANNEL_ID = "anime_broadcast_reminders"
        const val UNFINISHED_CHANNEL_ID = "anime_unfinished_reminders"
    }

    fun createNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val broadcastChannel = NotificationChannel(
            BROADCAST_CHANNEL_ID,
            "Airing Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for when your favorite anime is about to air."
            enableLights(true)
            enableVibration(true)
        }

        val unfinishedChannel = NotificationChannel(
            UNFINISHED_CHANNEL_ID,
            "Continue Watching",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to continue watching episodes you haven't finished."
        }

        notificationManager.createNotificationChannels(listOf(broadcastChannel, unfinishedChannel))
        Log.d("NotificationHandler", "Application notification channels created.")
    }

    suspend fun sendNotification(
        context: Context,
        notification: Notification,
        notificationId: Int
    ) = withContext(Dispatchers.IO) {
        val channelId = when (notification.type) {
            "Broadcast" -> BROADCAST_CHANNEL_ID
            "UnfinishedWatch" -> UNFINISHED_CHANNEL_ID
            else -> {
                log("Cannot send notification for unknown type: ${notification.type}")
                return@withContext
            }
        }

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

            else -> emptyList()
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications_active_black_24dp)
            .setContentText(notification.contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.contentText))
            .setPriority(if (notification.type == "Broadcast") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(
                createOpenIntent(
                    context,
                    notification.type,
                    notification.accessId,
                    notificationId
                )
            )
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
        }

        try {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.notify(notificationId, builder.build())
            log("Notification sent on channel $channelId for ${notification.contentText}")
        } catch (e: Exception) {
            log("Failed to send notification: ${e.message}")
        }
    }

    private fun createOpenIntent(
        context: Context,
        type: String,
        accessId: String,
        notificationId: Int
    ): PendingIntent {
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