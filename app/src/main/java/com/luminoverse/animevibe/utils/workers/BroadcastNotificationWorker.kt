package com.luminoverse.animevibe.utils.workers

import android.app.NotificationManager
import android.content.Context
import androidx.work.*
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.AnimeSchedulesSearchQueryState
import com.luminoverse.animevibe.models.Notification
import com.luminoverse.animevibe.repository.AnimeHomeRepository
import com.luminoverse.animevibe.repository.NotificationRepository
import com.luminoverse.animevibe.utils.factories.ChildWorkerFactory
import com.luminoverse.animevibe.utils.handlers.NotificationHandler
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.TimeUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class BroadcastNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val animeHomeRepository: AnimeHomeRepository,
    private val notificationHandler: NotificationHandler,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory : ChildWorkerFactory {
        override fun create(appContext: Context, params: WorkerParameters): BroadcastNotificationWorker
    }

    companion object {
        private const val WORK_NAME = "anime_broadcast_notification_work"
        private const val NOTIFICATION_WINDOW_MINUTES = 5L
        private const val MAX_RETRIES = 5

        private fun log(message: String) {
            println("BroadcastNotificationWorker: $message")
        }

        fun schedule(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.areNotificationsEnabled()) {
                log("Notifications disabled, skipping scheduling")
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<BroadcastNotificationWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            log("Scheduled broadcast notification worker (every 15 minutes)")
        }

        fun scheduleNow(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.areNotificationsEnabled()) {
                log("Notifications disabled, skipping immediate scheduling")
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<BroadcastNotificationWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("broadcast_notification")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
            log("Scheduled immediate broadcast notification")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val attempt = params.runAttemptCount + 1
        val currentTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
        log("Started at $currentTime, attempt $attempt of $MAX_RETRIES")

        if (attempt > MAX_RETRIES) {
            log("Max retries ($MAX_RETRIES) reached, failing")
            return@withContext Result.failure()
        }

        try {
            val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("notifications_enabled", true)) {
                log("Notifications disabled in settings, skipping")
                return@withContext Result.success()
            }

            notificationRepository.cleanOldNotifications()
            log("Cleaned old notifications")

            val schedules = fetchSchedules()
            log("Fetched ${schedules.size} anime schedules")
            processBroadcastNotifications(currentTime, schedules)

            log("Completed successfully at $currentTime")
            Result.success()
        } catch (e: Exception) {
            log("Worker error: ${e.message}, stacktrace=${e.stackTraceToString()}")
            Result.retry()
        }
    }

    private suspend fun fetchSchedules(): List<AnimeDetail> {
        val schedules = mutableListOf<AnimeDetail>()
        var page = 1
        try {
            while (true) {
                log("Fetching schedules page $page")
                val result = animeHomeRepository.getAnimeSchedules(AnimeSchedulesSearchQueryState(page = page))
                when (result) {
                    is Resource.Success -> {
                        schedules.addAll(result.data.data.filter { it.airing && it.broadcast.time != null && it.broadcast.timezone != null && it.broadcast.day != null })
                        if (!result.data.pagination.has_next_page) {
                            log("No more pages, total valid schedules: ${schedules.size}")
                            break
                        }
                        page++
                    }
                    is Resource.Error -> {
                        log("Failed to fetch schedules (page $page): ${result.message}")
                        throw Exception("Schedule fetch failed: ${result.message}")
                    }
                    else -> {
                        log("Unexpected response type for page $page")
                        throw Exception("Unexpected schedule response")
                    }
                }
            }
        } catch (e: Exception) {
            log("Error fetching schedules: ${e.message}")
            throw e
        }
        return schedules
    }

    private suspend fun processBroadcastNotifications(currentTime: ZonedDateTime, schedules: List<AnimeDetail>) {
        log("Starting processBroadcastNotifications with ${schedules.size} schedules")
        schedules.forEach { anime ->
            val broadcastTime = calculateBroadcastTime(anime) ?: return@forEach
            val timeUntilBroadcast = ChronoUnit.MINUTES.between(currentTime, broadcastTime)
            log("Anime: ${anime.title} (malId=${anime.mal_id}), broadcastTime=$broadcastTime, timeUntilBroadcast=$timeUntilBroadcast minutes")

            if (timeUntilBroadcast in 0..NOTIFICATION_WINDOW_MINUTES) {
                sendBroadcastNotification(anime)
            } else if (timeUntilBroadcast < 0) {
                log("Broadcast for ${anime.title} (malId=${anime.mal_id}) already passed, skipping")
            } else {
                log("Broadcast for ${anime.title} (malId=${anime.mal_id}) is too far in future, skipping")
            }
        }
        log("Finished processBroadcastNotifications")
    }

    private fun calculateBroadcastTime(anime: AnimeDetail): ZonedDateTime? {
        try {
            val time = anime.broadcast.time ?: run {
                log("Missing broadcast time for ${anime.title} (malId=${anime.mal_id}), skipping")
                return null
            }
            val timezone = anime.broadcast.timezone ?: run {
                log("Missing broadcast timezone for ${anime.title} (malId=${anime.mal_id}), skipping")
                return null
            }
            val day = anime.broadcast.day ?: run {
                log("Missing broadcast day for ${anime.title} (malId=${anime.mal_id}), skipping")
                return null
            }
            val broadcast = TimeUtils.getBroadcastDateTimeThisWeek(time, timezone, day)
                .withZoneSameInstant(ZoneId.of("Asia/Jakarta"))
            log("Calculated broadcast time for ${anime.title} (malId=${anime.mal_id}): $broadcast")
            return broadcast
        } catch (e: Exception) {
            log("Invalid broadcast data for ${anime.title} (malId=${anime.mal_id}): ${e.message}, skipping")
            return null
        }
    }

    private suspend fun sendBroadcastNotification(anime: AnimeDetail) {
        val accessId = anime.mal_id.toString()
        if (notificationRepository.checkDuplicateNotification(accessId, "Broadcast")) {
            log("Duplicate notification for ${anime.title} (accessId=$accessId), skipping")
            return
        }

        val notification = Notification(
            accessId = accessId,
            imageUrl = anime.images.webp.large_image_url ?: "",
            contentText = "${anime.title} is about to air!",
            type = "Broadcast"
        )
        try {
            val savedId = notificationRepository.saveNotification(notification)
            log("Saved Broadcast notification for ${anime.title} (accessId=$accessId, notificationId=$savedId)")

            notificationHandler.sendNotification(context, notification, accessId.hashCode())
            log("Notification sent for ${anime.title} (accessId=$accessId, notificationId=${accessId.hashCode()})")

            notificationRepository.getPendingNotifications()
                .find { it.accessId == accessId && it.type == "Broadcast" && it.id == savedId }
                ?.let {
                    notificationRepository.markNotificationAsSent(it.id)
                    log("Marked notification as sent for ${anime.title} (accessId=$accessId, notificationId=${it.id})")
                } ?: log("No pending notification found for ${anime.title} (accessId=$accessId)")
        } catch (e: Exception) {
            log("Failed to send notification for ${anime.title} (accessId=$accessId): ${e.message}, stacktrace=${e.stackTraceToString()}")
        }
    }
}