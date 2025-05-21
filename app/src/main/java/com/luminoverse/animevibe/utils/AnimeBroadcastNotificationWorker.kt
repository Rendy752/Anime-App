package com.luminoverse.animevibe.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
            val nextMidnight = if (now.toLocalTime().isBefore(LocalTime.MIDNIGHT)) {
                now.toLocalDate().atStartOfDay(ZoneId.of("Asia/Jakarta"))
            } else {
                now.toLocalDate().plusDays(1).atStartOfDay(ZoneId.of("Asia/Jakarta"))
            }
            val initialDelayMinutes = ChronoUnit.MINUTES.between(now, nextMidnight)

            val workRequest = PeriodicWorkRequestBuilder<AnimeBroadcastNotificationWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
            println("AnimeBroadcastNotificationWorker: Scheduled to run daily at midnight (Asia/Jakarta). Next run: $nextMidnight (in $initialDelayMinutes minutes)")
        }

        fun scheduleNow(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<AnimeBroadcastNotificationWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "$WORK_NAME-now",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            println("AnimeBroadcastNotificationWorker: Scheduled to run now with unique name $WORK_NAME-now")
        }

        private fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val attempt = params.runAttemptCount + 1
        println("AnimeBroadcastNotificationWorker: Started at ${ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))}, attempt $attempt of $MAX_RETRIES")

        if (attempt > MAX_RETRIES) {
            println("AnimeBroadcastNotificationWorker: Max retries ($MAX_RETRIES) reached, failing")
            return@withContext Result.failure()
        }

        val networkAvailable = isNetworkAvailable(context)
        println("AnimeBroadcastNotificationWorker: Network available: $networkAvailable")

        if (!networkAvailable) {
            println("AnimeBroadcastNotificationWorker: Network not available, retrying")
            return@withContext Result.retry()
        }

        try {
            val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
            println("AnimeBroadcastNotificationWorker: Notifications enabled: $notificationsEnabled")
            if (!notificationsEnabled) {
                println("AnimeBroadcastNotificationWorker: Notifications disabled, skipping")
                return@withContext Result.success()
            }

            val schedules = mutableListOf<AnimeDetail>()
            var page = 1
            val queryState = AnimeSchedulesSearchQueryState(page = page)

            while (true) {
                println("AnimeBroadcastNotificationWorker: Fetching schedules page $page")
                val scheduleResult = animeHomeRepository.getAnimeSchedules(queryState.copy(page = page))
                when (scheduleResult) {
                    is Resource.Success -> {
                        schedules.addAll(scheduleResult.data.data)
                        if (!scheduleResult.data.pagination.has_next_page) {
                            println("AnimeBroadcastNotificationWorker: No more pages, total schedules: ${schedules.size}")
                            break
                        }
                        page++
                    }
                    is Resource.Error -> {
                        println("AnimeBroadcastNotificationWorker: Failed to fetch schedules (page $page): ${scheduleResult.message}")
                        return@withContext Result.retry()
                    }
                    else -> {
                        println("AnimeBroadcastNotificationWorker: Unexpected response type for page $page")
                        return@withContext Result.retry()
                    }
                }
            }

            println("AnimeBroadcastNotificationWorker: Listing all scheduled anime:")
            schedules.forEach { anime ->
                println("  - malId: ${anime.mal_id}, Title: ${anime.title}, Broadcast: time=${anime.broadcast.time}, timezone=${anime.broadcast.timezone}, day=${anime.broadcast.day}, airing=${anime.airing}")
            }

            val currentTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
            println("AnimeBroadcastNotificationWorker: Current time: $currentTime")

            schedules
                .filter { it.airing }
                .forEach { anime ->
                    val broadcastTime = try {
                        val broadcast = TimeUtils.getBroadcastDateTimeThisWeek(
                            broadcastTime = anime.broadcast.time ?: run {
                                println("AnimeBroadcastNotificationWorker: Missing broadcast time for malId=${anime.mal_id}, skipping")
                                return@forEach
                            },
                            broadcastTimezone = anime.broadcast.timezone ?: run {
                                println("AnimeBroadcastNotificationWorker: Missing broadcast timezone for malId=${anime.mal_id}, skipping")
                                return@forEach
                            },
                            broadcastDay = anime.broadcast.day ?: run {
                                println("AnimeBroadcastNotificationWorker: Missing broadcast day for malId=${anime.mal_id}, skipping")
                                return@forEach
                            }
                        ).withZoneSameInstant(ZoneId.of("Asia/Jakarta"))
                        println("AnimeBroadcastNotificationWorker: Calculated broadcast time for ${anime.title} (malId=${anime.mal_id}): $broadcast")
                        broadcast
                    } catch (e: Exception) {
                        println("AnimeBroadcastNotificationWorker: Invalid broadcast data for malId=${anime.mal_id}: ${e.message}, skipping")
                        return@forEach
                    }

                    val timeUntilBroadcast = ChronoUnit.MINUTES.between(currentTime, broadcastTime)
                    println("AnimeBroadcastNotificationWorker: Time until broadcast for ${anime.title} (malId=${anime.mal_id}): $timeUntilBroadcast minutes")

                    if (timeUntilBroadcast >= 0 && timeUntilBroadcast <= NOTIFICATION_WINDOW_MINUTES) {
                        notificationHandler.sendNotification(
                            context = context,
                            type = NotificationType.Broadcast(
                                malId = anime.mal_id,
                                title = anime.title,
                                imageUrl = anime.images.webp.large_image_url ?: ""
                            )
                        )
                        println("AnimeBroadcastNotificationWorker: Notification sent for ${anime.title} (malId=${anime.mal_id}) at $currentTime, airing in $timeUntilBroadcast minutes")
                    } else if (timeUntilBroadcast < 0) {
                        println("AnimeBroadcastNotificationWorker: Broadcast for ${anime.title} (malId=${anime.mal_id}) already passed, skipping")
                    }
                }

            Result.success()
        } catch (e: Exception) {
            println("AnimeBroadcastNotificationWorker: Worker error: ${e.message}, stacktrace=${e.stackTraceToString()}")
            Result.retry()
        }
    }
}