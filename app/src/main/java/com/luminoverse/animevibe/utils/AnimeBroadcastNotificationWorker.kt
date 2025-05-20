package com.luminoverse.animevibe.utils

import android.content.Context
import androidx.work.*
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.AnimeSchedulesSearchQueryState
import com.luminoverse.animevibe.repository.AnimeHomeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class AnimeBroadcastNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val animeHomeRepository: AnimeHomeRepository,
    private val notificationHandler: NotificationHandler
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory : ChildWorkerFactory {
        override fun create(
            appContext: Context,
            params: WorkerParameters
        ): AnimeBroadcastNotificationWorker
    }

    companion object {
        const val WORK_NAME = "anime_broadcast_notification_work"
        private const val NOTIFICATION_WINDOW_MINUTES = 5L
        private const val MAX_RETRIES = 3

        fun schedule(context: Context) {
            val now = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
            val nextMidnight = now
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .plusDays(if (now.hour >= 0) 1 else 0)
            val delay = ChronoUnit.MILLIS.between(now, nextMidnight)

            val workRequest = PeriodicWorkRequestBuilder<AnimeBroadcastNotificationWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    10,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            println("AnimeBroadcastNotificationWorker: Scheduled for $nextMidnight, delay=$delay ms")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            println("AnimeBroadcastNotificationWorker: Canceled")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        println("AnimeBroadcastNotificationWorker: Started at ${ZonedDateTime.now()}, attempt ${params.runAttemptCount}")
        if (params.runAttemptCount > MAX_RETRIES) {
            println("AnimeBroadcastNotificationWorker: Max retries ($MAX_RETRIES) reached")
            return@withContext Result.failure()
        }

        try {
            val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("notifications_enabled", true)) {
                println("AnimeBroadcastNotificationWorker: Notifications disabled")
                return@withContext Result.success()
            }

            val schedules = mutableListOf<AnimeDetail>()
            var page = 1
            val queryState = AnimeSchedulesSearchQueryState(page = page)

            while (true) {
                println("AnimeBroadcastNotificationWorker: Fetching schedules page $page")
                val scheduleResult =
                    animeHomeRepository.getAnimeSchedules(queryState.copy(page = page))
                if (scheduleResult !is Resource.Success) {
                    println("AnimeBroadcastNotificationWorker: Failed to fetch schedules (page $page): ${scheduleResult.message}")
                    return@withContext Result.retry()
                }

                schedules.addAll(scheduleResult.data.data)
                if (!scheduleResult.data.pagination.has_next_page) {
                    println("AnimeBroadcastNotificationWorker: No more pages, total schedules: ${schedules.size}")
                    break
                }
                page++
            }

            val currentTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
            schedules
                .filter { it.airing }
                .forEach { anime ->
                    val broadcastTime = try {
                        TimeUtils.getBroadcastDateTimeThisWeek(
                            broadcastTime = anime.broadcast.time ?: return@forEach,
                            broadcastTimezone = anime.broadcast.timezone ?: return@forEach,
                            broadcastDay = anime.broadcast.day ?: return@forEach
                        ).withZoneSameInstant(ZoneId.of("Asia/Jakarta"))
                    } catch (e: Exception) {
                        println("AnimeBroadcastNotificationWorker: Invalid broadcast data for malId=${anime.mal_id}: ${e.message}")
                        return@forEach
                    }

                    val timeUntilBroadcast = ChronoUnit.MINUTES.between(currentTime, broadcastTime)
                    if (timeUntilBroadcast in 0..NOTIFICATION_WINDOW_MINUTES) {
                        notificationHandler.sendNotification(
                            context = context,
                            type = NotificationType.Broadcast(
                                malId = anime.mal_id,
                                title = anime.title,
                                imageUrl = anime.images.webp.large_image_url
                            )
                        )
                        println("AnimeBroadcastNotificationWorker: Notification sent for ${anime.title} (malId: ${anime.mal_id}) at ${currentTime}, airing in $timeUntilBroadcast minutes")
                    }
                }

            Result.success()
        } catch (e: Exception) {
            println("AnimeBroadcastNotificationWorker: Worker error: ${e.message}, stacktrace=${e.stackTraceToString()}")
            Result.retry()
        }
    }
}