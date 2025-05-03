package com.example.animeapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import com.example.animeapp.data.local.dao.AnimeDetailComplementDao
import com.example.animeapp.data.local.dao.AnimeDetailDao
import java.util.concurrent.TimeUnit
import androidx.core.content.edit
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class AnimeBroadcastNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val animeDetailComplementDao: AnimeDetailComplementDao,
    private val animeDetailDao: AnimeDetailDao
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory : ChildWorkerFactory {
        override fun create(appContext: Context, params: WorkerParameters): AnimeBroadcastNotificationWorker
    }

    companion object {
        const val CHANNEL_ID = "anime_notifications"
        const val WORK_NAME = "anime_notification_work"
        private const val PREFS_NOTIFICATIONS_SENT = "notifications_sent"
        private const val NOTIFICATION_WINDOW_HOURS = 24L

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<AnimeBroadcastNotificationWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            println("AnimeBroadcastNotificationWorker: Scheduled with 1-hour interval")
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
                    if (timeUntilBroadcast in 0..60) {
                        val sentNotifications =
                            settingsPrefs.getStringSet(PREFS_NOTIFICATIONS_SENT, emptySet())
                                ?.toMutableSet() ?: mutableSetOf()
                        val notificationKey = "${complement.malId}_${broadcastTime.toLocalDate()}"
                        if (!sentNotifications.contains(notificationKey)) {
                            sendNotification(
                                malId = complement.malId,
                                title = animeDetail.title
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
        val descriptionText = "Notifications for airing anime"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        println("AnimeBroadcastNotificationWorker: Notification channel created: $CHANNEL_ID")
    }

    private fun sendNotification(malId: Int, title: String) {
        val intent = Intent(Intent.ACTION_VIEW, "animeapp://anime/detail/$malId".toUri())
        val pendingIntent = PendingIntent.getActivity(
            context,
            malId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(androidx.media3.session.R.drawable.media3_notification_small_icon)
            .setContentTitle("Anime On Air")
            .setContentText("$title is airing now!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(malId, notification)
        println("AnimeBroadcastNotificationWorker: Notification sent for $title (malId: $malId)")
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