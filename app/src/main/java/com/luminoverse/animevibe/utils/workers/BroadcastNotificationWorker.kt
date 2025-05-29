package com.luminoverse.animevibe.utils.workers

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.work.*
import com.luminoverse.animevibe.AnimeApplication
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.AnimeSchedulesSearchQueryState
import com.luminoverse.animevibe.models.Notification
import com.luminoverse.animevibe.repository.AnimeHomeRepository
import com.luminoverse.animevibe.repository.NotificationRepository
import com.luminoverse.animevibe.utils.TimeUtils
import com.luminoverse.animevibe.utils.factories.ChildWorkerFactory
import com.luminoverse.animevibe.utils.handlers.NotificationHandler
import com.luminoverse.animevibe.utils.receivers.ServiceRestartReceiver
import com.luminoverse.animevibe.utils.resource.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*
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
        private const val MAX_RETRIES = 3
        private const val PREFS_NAME = "BroadcastNotificationPrefs"
        private const val KEY_LAST_FETCH = "last_successful_fetch"
        const val ACTION_RESTART_SERVICE = "com.luminoverse.animevibe.RESTART_MEDIA_SERVICE"

        private fun log(message: String) {
            println("BroadcastNotificationWorker: $message")
        }

        fun schedule(context: Context) {
            val notificationManager = context.getSystemService<NotificationManager>()
            if (!notificationManager?.areNotificationsEnabled()!!) {
                log("Notifications disabled, skipping scheduling")
                return
            }

            val workManager = WorkManager.getInstance(context)
            val workInfo = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
            if (workInfo.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }) {
                log("Work $WORK_NAME already scheduled, skipping")
                return
            }

            val currentTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
            val midnight = currentTime.toLocalDate().plusDays(1).atStartOfDay(ZoneId.of("Asia/Jakarta"))
            val initialDelay = Duration.between(currentTime, midnight).toMillis()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<BroadcastNotificationWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            log("Scheduled broadcast notification worker to start at $midnight (initial delay: ${initialDelay}ms)")
        }

        private fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        fun setLastFetchTime(context: Context, time: Long) {
            getSharedPreferences(context).edit { putLong(KEY_LAST_FETCH, time) }
        }

        fun getLastFetchTime(context: Context): Long {
            return getSharedPreferences(context).getLong(KEY_LAST_FETCH, 0)
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
            val notificationManager = context.getSystemService<NotificationManager>()
            if (!notificationManager?.areNotificationsEnabled()!!) {
                log("Notifications disabled, retrying")
                return@withContext Result.retry()
            }

            val currentLocalTime = currentTime.toLocalTime()
            val isMidnightWindow = currentLocalTime.isAfter(LocalTime.of(23, 45)) ||
                    currentLocalTime.isBefore(LocalTime.of(0, 15))
            if (!isMidnightWindow) {
                log("Outside midnight window (current time: $currentLocalTime), skipping processing")
                return@withContext Result.success()
            }

            val lastFetchTime = getLastFetchTime(context)
            val lastFetchDate = if (lastFetchTime > 0) {
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastFetchTime), ZoneId.of("Asia/Jakarta"))
            } else null
            if (lastFetchDate != null && lastFetchDate.toLocalDate() == currentTime.toLocalDate()) {
                log("Schedules already fetched today at $lastFetchDate, skipping fetch")
                return@withContext Result.success()
            }

            val app = context.applicationContext as? AnimeApplication
            val wasServiceRunning = app?.stopMediaServiceForWorker() == true
            log("Stopped MediaPlaybackService: $wasServiceRunning")

            notificationRepository.cleanOldNotifications()
            log("Cleaned old notifications")

            val schedules = fetchSchedules()
            log("Fetched ${schedules.size} anime schedules")
            setLastFetchTime(context, currentTime.toInstant().toEpochMilli())
            processBroadcastNotifications(currentTime, schedules)

            if (wasServiceRunning) {
                log("Requesting to restart MediaPlaybackService")
                val intent = Intent(ACTION_RESTART_SERVICE).apply {
                    component = ComponentName(context, ServiceRestartReceiver::class.java)
                }
                context.sendBroadcast(intent)
            }

            log("Completed successfully at $currentTime")
            Result.success()
        } catch (e: Exception) {
            log("Error: ${e.javaClass.name}, message=${e.message}, stacktrace=${e.stackTraceToString()}")
            return@withContext if (attempt < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private suspend fun fetchSchedules(): List<AnimeDetail> {
        val schedules = mutableListOf<AnimeDetail>()
        var page = 1
        try {
            val connectivityManager = context.getSystemService<ConnectivityManager>()
            val network = connectivityManager?.activeNetwork
            val networkCapabilities = connectivityManager?.getNetworkCapabilities(network)
            val isConnected = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            if (!isConnected) {
                log("No internet connection, will try to use cached schedules")
                // Implement cache retrieval logic here if available
                return schedules
            }

            while (true) {
                log("Fetching schedules page $page")
                val result = animeHomeRepository.getAnimeSchedules(AnimeSchedulesSearchQueryState(page = page))
                when (result) {
                    is Resource.Success -> {
                        schedules.addAll(result.data.data.filter {
                            it.airing && it.broadcast.time != null && it.broadcast.timezone != null && it.broadcast.day != null
                        })
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
        log("Processing ${schedules.size} schedules")
        schedules.forEach { anime ->
            val broadcastTime = calculateBroadcastTime(anime) ?: return@forEach
            val timeUntilBroadcast = Duration.between(currentTime, broadcastTime).toMinutes()
            log("Anime: ${anime.title} (malId=${anime.mal_id}), broadcastTime=$broadcastTime, timeUntilBroadcast=$timeUntilBroadcast minutes")

            if (timeUntilBroadcast in 0..NOTIFICATION_WINDOW_MINUTES) {
                sendBroadcastNotification(anime)
            } else if (timeUntilBroadcast > NOTIFICATION_WINDOW_MINUTES) {
                scheduleFutureNotification(anime, broadcastTime)
            } else {
                log("Broadcast for ${anime.title} (malId=${anime.mal_id}) already passed, skipping")
            }
        }
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
            log("Saved notification: ${anime.title} (accessId=$accessId, notificationId=$savedId)")
            notificationHandler.sendNotification(context, notification, accessId.hashCode())
            notificationRepository.markNotificationAsSent(savedId)
            log("Sent notification for ${anime.title} (accessId=$accessId)")
        } catch (e: Exception) {
            log("Failed to send notification for ${anime.title}: ${e.message}, stacktrace=${e.stackTraceToString()}")
        }
    }

    private suspend fun scheduleFutureNotification(anime: AnimeDetail, broadcastTime: ZonedDateTime) {
        val accessId = anime.mal_id.toString()
        if (notificationRepository.checkDuplicateNotification(accessId, "Broadcast")) {
            log("Duplicate scheduled notification for ${anime.title} (accessId=$accessId), skipping")
            return
        }

        val notification = Notification(
            accessId = accessId,
            imageUrl = anime.images.webp.large_image_url ?: "",
            contentText = "${anime.title} is about to air!",
            type = "Broadcast"
        )
        val savedId = notificationRepository.saveNotification(notification)
        log("Saved notification for scheduling: ${anime.title} (accessId=$accessId, notificationId=$savedId)")

        val currentTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
        val delay = Duration.between(currentTime, broadcastTime).toMillis().coerceAtLeast(0)

        val workRequest = OneTimeWorkRequestBuilder<NotificationDeliveryWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .setInputData(
                workDataOf(
                    "notification_id" to savedId,
                    "access_id" to accessId,
                    "content_text" to notification.contentText,
                    "image_url" to notification.imageUrl,
                    "type" to notification.type
                )
            )
            .addTag("broadcast_notification_$accessId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "broadcast_notification_$accessId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
        log("Scheduled notification for ${anime.title} at $broadcastTime (delay: ${delay}ms)")
    }
}