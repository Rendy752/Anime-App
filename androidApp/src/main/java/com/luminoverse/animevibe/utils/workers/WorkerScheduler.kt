package com.luminoverse.animevibe.utils.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.Notification
import com.luminoverse.animevibe.repository.NotificationRepository
import com.luminoverse.animevibe.utils.TimeUtils
import com.luminoverse.animevibe.utils.handlers.NotificationHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val NOTIFICATION_WINDOW_MINUTES = 5L
private const val BROADCAST_WORK_NAME_PREFIX = "broadcast_notification_"

@Singleton
class WorkerScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository,
    private val notificationHandler: NotificationHandler
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleBroadcastNotifications() {
        val currentTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
        val midnight = currentTime.toLocalDate().plusDays(1).atStartOfDay(ZoneId.of("Asia/Jakarta"))
        val initialDelay = Duration.between(currentTime, midnight)

        val workRequest = PeriodicWorkRequestBuilder<BroadcastNotificationWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "anime_broadcast_notification_work",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        Log.d("WorkerScheduler", "BroadcastNotificationWorker scheduled to run at: $midnight")
    }

    fun scheduleUnfinishedWatchNotifications() {
        val targetTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
        val currentTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))

        targetTimes.forEachIndexed { index, targetTime ->
            val uniqueWorkName = "anime_unfinished_notification_work_$index"
            var adjustedTargetTime = currentTime.with(targetTime)
            if (currentTime.isAfter(adjustedTargetTime)) {
                adjustedTargetTime = adjustedTargetTime.plusDays(1)
            }
            val initialDelay = Duration.between(currentTime, adjustedTargetTime)
            val workRequest = PeriodicWorkRequestBuilder<UnfinishedWatchNotificationWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            ).setInitialDelay(initialDelay).build()
            workManager.enqueueUniquePeriodicWork(
                uniqueWorkName,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
            Log.d(
                "WorkerScheduler",
                "UnfinishedWatchNotificationWorker for $targetTime scheduled to run at: $adjustedTargetTime"
            )
        }
    }


    suspend fun processAndScheduleBroadcasts(schedules: List<AnimeDetail>) {
        val currentTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
        schedules.forEach { anime ->
            val broadcastTime = calculateBroadcastTime(anime) ?: return@forEach
            val timeUntilBroadcast = Duration.between(currentTime, broadcastTime).toMinutes()

            if (timeUntilBroadcast in 0..NOTIFICATION_WINDOW_MINUTES) {
                sendBroadcastNotification(anime)
            } else if (timeUntilBroadcast > NOTIFICATION_WINDOW_MINUTES) {
                scheduleFutureNotification(anime, broadcastTime)
            }
        }
    }

    suspend fun scheduleImmediateBroadcastNotification(anime: AnimeDetail) {
        Log.d("WorkerScheduler", "Running immediate schedule check for ${anime.title}")
        val broadcastTime = calculateBroadcastTime(anime) ?: return
        val timeUntilBroadcast =
            Duration.between(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")), broadcastTime)

        if (!timeUntilBroadcast.isNegative) {
            scheduleFutureNotification(anime, broadcastTime)
        } else {
            Log.d(
                "WorkerScheduler",
                "Immediate check for ${anime.title}: broadcast time has already passed."
            )
        }
    }

    suspend fun cancelImmediateBroadcastNotification(malId: Int) {
        val accessId = malId.toString()
        val uniqueWorkName = "$BROADCAST_WORK_NAME_PREFIX$accessId"

        workManager.cancelUniqueWork(uniqueWorkName)

        notificationRepository.deleteNotificationByAccessId(accessId, "Broadcast")

        Log.d("WorkerScheduler", "Cancelled work and deleted notification entry for malId: $malId")
    }


    private fun calculateBroadcastTime(anime: AnimeDetail): ZonedDateTime? {
        try {
            val time = anime.broadcast.time ?: return null
            val timezone = anime.broadcast.timezone ?: return null
            val day = anime.broadcast.day ?: return null
            return TimeUtils.getBroadcastDateTimeThisWeek(time, timezone, day)
                .withZoneSameInstant(ZoneId.of("Asia/Jakarta"))
        } catch (e: Exception) {
            Log.e("WorkerScheduler", "Invalid broadcast data for ${anime.title}: ${e.message}")
            return null
        }
    }

    private suspend fun sendBroadcastNotification(anime: AnimeDetail) {
        val accessId = anime.mal_id.toString()
        if (notificationRepository.checkDuplicateNotification(accessId, "Broadcast")) return

        val notification = Notification(
            accessId = accessId,
            imageUrl = anime.images.webp.large_image_url ?: "",
            contentText = "${anime.title} is about to air!",
            type = "Broadcast"
        )
        try {
            val savedId = notificationRepository.saveNotification(notification)
            notificationHandler.sendNotification(context, notification, savedId.toInt())
            notificationRepository.markNotificationAsSent(savedId)
            Log.d("WorkerScheduler", "Sent immediate notification for ${anime.title}")
        } catch (e: Exception) {
            Log.e("WorkerScheduler", "Failed to send notification for ${anime.title}", e)
        }
    }

    private suspend fun scheduleFutureNotification(
        anime: AnimeDetail,
        broadcastTime: ZonedDateTime
    ) {
        val accessId = anime.mal_id.toString()
        if (notificationRepository.checkDuplicateNotification(accessId, "Broadcast")) {
            Log.d(
                "WorkerScheduler",
                "Future notification for ${anime.title} already exists, skipping."
            )
            return
        }

        val notification = Notification(
            accessId = accessId,
            imageUrl = anime.images.webp.large_image_url ?: "",
            contentText = "${anime.title} is about to air!",
            type = "Broadcast"
        )
        val savedId = notificationRepository.saveNotification(notification)
        val delay =
            Duration.between(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")), broadcastTime).toMillis()
                .coerceAtLeast(0)

        val workRequest = OneTimeWorkRequestBuilder<NotificationDeliveryWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    "notification_id" to savedId,
                    "access_id" to accessId,
                    "content_text" to notification.contentText,
                    "image_url" to notification.imageUrl,
                    "type" to notification.type
                )
            )
            .build()

        workManager.enqueueUniqueWork(
            "$BROADCAST_WORK_NAME_PREFIX$accessId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        Log.d(
            "WorkerScheduler",
            "Scheduled future notification for ${anime.title} at $broadcastTime"
        )
    }
}