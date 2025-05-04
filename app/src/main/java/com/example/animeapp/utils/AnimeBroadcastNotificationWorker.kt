package com.example.animeapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<AnimeBroadcastNotificationWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            println("AnimeBroadcastNotificationWorker: Scheduled with 15-minute interval")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        println("AnimeBroadcastNotificationWorker: Started at ${ZonedDateTime.now()}")
        try {
            val settingsPrefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            val notificationEnabled = settingsPrefs.getBoolean("notifications_enabled", true)
            println("AnimeBroadcastNotificationWorker: notificationEnabled=$notificationEnabled")
            if (!notificationEnabled) {
                println("AnimeBroadcastNotificationWorker: Notifications disabled, skipping worker")
                return@withContext Result.success()
            }

            createNotificationChannel()

            val favorites = animeDetailComplementDao.getAllFavorites()
            println("AnimeBroadcastNotificationWorker: Favorites count: ${favorites.size}")
            if (favorites.isEmpty()) {
                println("AnimeBroadcastNotificationWorker: No favorites found, skipping notification checks")
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
                    println("AnimeBroadcastNotificationWorker: AnimeDetail not found for malId=${complement.malId}")
                    return@forEach
                }
                if (animeDetail.airing != true) {
                    println("AnimeBroadcastNotificationWorker: Anime not airing: malId=${complement.malId}, title=${animeDetail.title}")
                    return@forEach
                }
                if (animeDetail.broadcast.time == null || animeDetail.broadcast.day == null || animeDetail.broadcast.timezone == null) {
                    println("AnimeBroadcastNotificationWorker: Incomplete broadcast data for malId=${complement.malId}, title=${animeDetail.title}, time=${animeDetail.broadcast.time}, day=${animeDetail.broadcast.day}, timezone=${animeDetail.broadcast.timezone}")
                    hasIncompleteData = true
                    return@forEach
                }

                try {
                    val broadcastTime = TimeUtils.getBroadcastDateTimeThisWeek(
                        broadcastTime = animeDetail.broadcast.time,
                        broadcastTimezone = animeDetail.broadcast.timezone,
                        broadcastDay = animeDetail.broadcast.day
                    ).withZoneSameInstant(ZoneId.systemDefault())
                    println("AnimeBroadcastNotificationWorker: Broadcast time for ${animeDetail.title}: $broadcastTime")

                    val timeUntilBroadcast = ChronoUnit.MINUTES.between(currentTime, broadcastTime)
                    println("AnimeBroadcastNotificationWorker: Checking ${animeDetail.title}: time until broadcast = $timeUntilBroadcast minutes")
                    if (timeUntilBroadcast in 0..NOTIFICATION_WINDOW_MINUTES) {
                        val sentNotifications =
                            settingsPrefs.getStringSet(PREFS_NOTIFICATIONS_SENT, emptySet())
                                ?.toMutableSet() ?: mutableSetOf()
                        val notificationKey = "${complement.malId}_${broadcastTime.toLocalDate()}"
                        if (!sentNotifications.contains(notificationKey)) {
                            sendNotification(
                                malId = complement.malId,
                                title = animeDetail.title,
                                imageUrl = animeDetail.images.webp.large_image_url
                            )
                            sentNotifications.add(notificationKey)
                            settingsPrefs.edit {
                                putStringSet(
                                    PREFS_NOTIFICATIONS_SENT,
                                    sentNotifications
                                )
                            }
                            println("AnimeBroadcastNotificationWorker: Recorded notification sent for malId=${complement.malId}, date=${broadcastTime.toLocalDate()}")
                        } else {
                            println("AnimeBroadcastNotificationWorker: Skipped duplicate notification for malId=${complement.malId}, date=${broadcastTime.toLocalDate()}")
                        }
                    }
                } catch (e: Exception) {
                    println("AnimeBroadcastNotificationWorker: Error processing broadcast time for malId=${complement.malId}, title=${animeDetail.title}: ${e.message}")
                    hasIncompleteData = true
                }
            }

            cleanUpSentNotifications(settingsPrefs, currentTime)

            if (hasIncompleteData) {
                println("AnimeBroadcastNotificationWorker: Some anime have incomplete broadcast data; consider refreshing data")
            }

            Result.success()
        } catch (e: Exception) {
            println("AnimeBroadcastNotificationWorker: Worker error: ${e.message}")
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        val name = "Broadcast Notifications"
        val descriptionText = "Notifications for anime about to air"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        println("AnimeBroadcastNotificationWorker: Notification channel created: $CHANNEL_ID, description=$descriptionText")
    }

    private suspend fun sendNotification(malId: Int, title: String, imageUrl: String?) {
        val intent = Intent(Intent.ACTION_VIEW, "animeapp://anime/detail/$malId".toUri())
        val pendingIntent = PendingIntent.getActivity(
            context,
            malId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_active_black_24dp)
            .setContentTitle("Anime About to Air")
            .setContentText("$title is about to air!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (imageUrl != null) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(512, 512)
                    .allowHardware(false)
                    .build()
                val bitmap = context.imageLoader.execute(request).drawable?.toBitmap()
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                        .setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .bigLargeIcon(null as Bitmap?)
                        )
                    println("AnimeBroadcastNotificationWorker: Loaded WebP image for notification: $imageUrl")
                } else {
                    println("AnimeBroadcastNotificationWorker: Failed to load WebP image for notification: $imageUrl")
                }
            } catch (e: Exception) {
                println("AnimeBroadcastNotificationWorker: Error loading WebP image for notification: $imageUrl, error=${e.message}")
            }
        }

        val notification = builder.build()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(malId, notification)
        println("AnimeBroadcastNotificationWorker: Notification sent for $title (malId: $malId, title='Anime About to Air', text='$title is about to air!')")
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
                    println("AnimeBroadcastNotificationWorker: Removed old notification record: $key")
                }
            } catch (e: Exception) {
                println("AnimeBroadcastNotificationWorker: Error cleaning up notification record: $key, ${e.message}")
                iterator.remove()
            }
        }
        prefs.edit { putStringSet(PREFS_NOTIFICATIONS_SENT, sentNotifications) }
    }
}