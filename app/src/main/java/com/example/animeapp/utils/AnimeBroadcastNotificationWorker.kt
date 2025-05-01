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
import javax.inject.Inject
import com.example.animeapp.data.local.dao.AnimeDetailComplementDao
import com.example.animeapp.data.local.dao.AnimeDetailDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit

class AnimeBroadcastNotificationWorker @Inject constructor(
    @ApplicationContext private val context: Context,
    params: WorkerParameters,
    private val animeDetailComplementDao: AnimeDetailComplementDao,
    private val animeDetailDao: AnimeDetailDao
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "anime_notifications"
        const val WORK_NAME = "anime_notification_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<AnimeBroadcastNotificationWorker>()
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
            println("AnimeBroadcastNotificationWorker: Scheduled with 5-minute interval")
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
                reEnqueueWorker()
                return@withContext Result.success()
            }

            createNotificationChannel()

            val favorites = animeDetailComplementDao.getAllFavorites()
            println("AnimeBroadcastNotificationWorker: Favorites count: ${favorites.size}")
            if (favorites.isEmpty()) {
                println("AnimeBroadcastNotificationWorker: No favorites found, skipping notification checks")
                reEnqueueWorker()
                return@withContext Result.success()
            }

            val currentTime = ZonedDateTime.now(ZoneId.systemDefault())
            var hasIncompleteData = false
            favorites.forEach { complement ->
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
                    if (timeUntilBroadcast in 0..5) {
                        sendNotification(
                            malId = complement.malId,
                            title = animeDetail.title
                        )
                    }
                } catch (e: Exception) {
                    println("AnimeBroadcastNotificationWorker: Error processing broadcast time for malId=${complement.malId}, title=${animeDetail.title}: ${e.message}")
                    hasIncompleteData = true
                }
            }

            if (hasIncompleteData) {
                println("AnimeBroadcastNotificationWorker: Some anime have incomplete broadcast data; consider refreshing data")
            }

            reEnqueueWorker()
            Result.success()
        } catch (e: Exception) {
            println("AnimeBroadcastNotificationWorker: Worker error: ${e.message}")
            reEnqueueWorker()
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        val name = "Broadcast Notifications"
        val descriptionText = "Notifications for airing anime"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(malId, notification)
        println("AnimeBroadcastNotificationWorker: Notification sent for $title (malId: $malId)")
    }

    private fun reEnqueueWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AnimeBroadcastNotificationWorker>()
            .setConstraints(constraints)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        println("AnimeBroadcastNotificationWorker: Re-enqueued for next run in 5 minutes")
    }
}