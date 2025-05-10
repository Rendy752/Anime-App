package com.example.animeapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.work.*
import coil.imageLoader
import coil.request.ImageRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import androidx.core.content.edit
import com.example.animeapp.data.local.dao.AnimeDetailComplementDao
import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.R
import dagger.assisted.AssistedFactory

class AnimeBroadcastNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val animeDetailComplementDao: AnimeDetailComplementDao,
    private val animeDetailDao: AnimeDetailDao
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory : ChildWorkerFactory {
        override fun create(
            appContext: Context,
            params: WorkerParameters
        ): AnimeBroadcastNotificationWorker
    }

    companion object {
        const val CHANNEL_ID = "anime_notifications"
        const val WORK_NAME = "anime_notification_work"
        private const val PREFS_NOTIFICATIONS_SENT = "notifications_sent"
        private const val NOTIFICATION_WINDOW_HOURS = 24L
        private const val NOTIFICATION_WINDOW_MINUTES = 5L

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<AnimeBroadcastNotificationWorker>(
                repeatInterval = 15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, workRequest
            )
            println("AnimeBroadcastNotificationWorker: Scheduled")
        }

        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Broadcast Notifications", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Anime airing notifications" }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
            println("AnimeBroadcastNotificationWorker: Channel created: $CHANNEL_ID")
        }

        suspend fun sendNotification(
            context: Context,
            malId: Int,
            title: String,
            imageUrl: String?
        ) {
            val openIntent = Intent(Intent.ACTION_VIEW, "animeapp://anime/detail/$malId".toUri())
            val openPendingIntent = PendingIntent.getActivity(
                context,
                malId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val closeIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = "ACTION_CLOSE_NOTIFICATION"
                putExtra("notification_id", malId)
            }
            val closePendingIntent = PendingIntent.getBroadcast(
                context,
                malId + 1,
                closeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_active_black_24dp)
                .setContentTitle("Anime About to Air")
                .setContentText("$title is about to air!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_open_in_new_black_24dp, "Detail", openPendingIntent)
                .addAction(R.drawable.ic_close_black_24dp, "Close", closePendingIntent)

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
                    println("AnimeBroadcastNotificationWorker: Error loading image: $imageUrl, error=${e.message}")
                }
            }

            context.getSystemService(NotificationManager::class.java).notify(malId, builder.build())
            println("AnimeBroadcastNotificationWorker: Notification sent for $title (malId: $malId)")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        println("AnimeBroadcastNotificationWorker: Started at ${ZonedDateTime.now()}")
        try {
            val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("notifications_enabled", true)) {
                println("AnimeBroadcastNotificationWorker: Notifications disabled")
                return@withContext Result.success()
            }

            createNotificationChannel(context)

            val favorites = animeDetailComplementDao.getAllFavorites()
            println("AnimeBroadcastNotificationWorker: Favorites count: ${favorites.size}")
            if (favorites.isEmpty()) {
                println("AnimeBroadcastNotificationWorker: No favorites found")
                return@withContext Result.success()
            }

            val currentTime = ZonedDateTime.now(ZoneId.systemDefault())
            var hasIncompleteData = false
            favorites.forEach { complement ->
                if (!complement.isFavorite) {
                    println("AnimeBroadcastNotificationWorker: Skipping non-favorite: malId=${complement.malId}")
                    return@forEach
                }
                val animeDetail = animeDetailDao.getAnimeDetailById(complement.malId)
                if (animeDetail == null) {
                    println("AnimeBroadcastNotificationWorker: AnimeDetail not found: malId=${complement.malId}")
                    return@forEach
                }
                if (!animeDetail.airing) {
                    println("AnimeBroadcastNotificationWorker: Anime not airing: malId=${complement.malId}")
                    return@forEach
                }
                if (animeDetail.broadcast.time == null || animeDetail.broadcast.day == null || animeDetail.broadcast.timezone == null) {
                    println("AnimeBroadcastNotificationWorker: Incomplete broadcast data: malId=${complement.malId}")
                    hasIncompleteData = true
                    return@forEach
                }

                try {
                    val broadcastTime = TimeUtils.getBroadcastDateTimeThisWeek(
                        broadcastTime = animeDetail.broadcast.time,
                        broadcastTimezone = animeDetail.broadcast.timezone,
                        broadcastDay = animeDetail.broadcast.day
                    ).withZoneSameInstant(ZoneId.systemDefault())
                    val timeUntilBroadcast = ChronoUnit.MINUTES.between(currentTime, broadcastTime)
                    println("AnimeBroadcastNotificationWorker: Checking ${animeDetail.title}: $timeUntilBroadcast min")
                    if (timeUntilBroadcast in 0..NOTIFICATION_WINDOW_MINUTES) {
                        val sentNotifications =
                            prefs.getStringSet(PREFS_NOTIFICATIONS_SENT, emptySet())?.toMutableSet()
                                ?: mutableSetOf()
                        val notificationKey = "${complement.malId}_${broadcastTime.toLocalDate()}"
                        if (!sentNotifications.contains(notificationKey)) {
                            sendNotification(
                                context,
                                complement.malId,
                                animeDetail.title,
                                animeDetail.images.webp.large_image_url
                            )
                            sentNotifications.add(notificationKey)
                            prefs.edit { putStringSet(PREFS_NOTIFICATIONS_SENT, sentNotifications) }
                            println("AnimeBroadcastNotificationWorker: Notification sent: malId=${complement.malId}")
                        } else {
                            println("AnimeBroadcastNotificationWorker: Skipped duplicate: malId=${complement.malId}")
                        }
                    }
                } catch (e: Exception) {
                    println("AnimeBroadcastNotificationWorker: Error for malId=${complement.malId}: ${e.message}")
                    hasIncompleteData = true
                }
            }

            cleanUpSentNotifications(prefs, currentTime)

            if (hasIncompleteData) {
                println("AnimeBroadcastNotificationWorker: Incomplete data detected")
            }

            Result.success()
        } catch (e: Exception) {
            println("AnimeBroadcastNotificationWorker: Worker error: ${e.message}")
            Result.retry()
        }
    }

    private fun cleanUpSentNotifications(
        prefs: android.content.SharedPreferences,
        currentTime: ZonedDateTime
    ) {
        val sentNotifications =
            prefs.getStringSet(PREFS_NOTIFICATIONS_SENT, emptySet())?.toMutableSet()
                ?: mutableSetOf()
        val iterator = sentNotifications.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            try {
                val dateStr = key.substringAfterLast("_")
                val notificationDate = ZonedDateTime.parse(dateStr + "T00:00:00Z").toLocalDate()
                if (ChronoUnit.HOURS.between(
                        notificationDate.atStartOfDay(ZoneId.systemDefault()),
                        currentTime
                    ) > NOTIFICATION_WINDOW_HOURS
                ) {
                    iterator.remove()
                    println("AnimeBroadcastNotificationWorker: Removed old notification: $key")
                }
            } catch (e: Exception) {
                println("AnimeBroadcastNotificationWorker: Error cleaning notification: $key, ${e.message}")
                iterator.remove()
            }
        }
        prefs.edit { putStringSet(PREFS_NOTIFICATIONS_SENT, sentNotifications) }
    }
}