package com.luminoverse.animevibe.utils.workers

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.work.*
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.Notification
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.repository.NotificationRepository
import com.luminoverse.animevibe.utils.factories.ChildWorkerFactory
import com.luminoverse.animevibe.utils.handlers.NotificationHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*
import java.util.concurrent.TimeUnit

class UnfinishedWatchNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val notificationRepository: NotificationRepository,
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory : ChildWorkerFactory {
        override fun create(appContext: Context, params: WorkerParameters): UnfinishedWatchNotificationWorker
    }

    companion object {
        private const val WORK_NAME = "anime_unfinished_notification_work"
        private const val MAX_RETRIES = 3

        private fun log(message: String) {
            println("UnfinishedWatchNotificationWorker: $message")
        }

        fun schedule(context: Context) {
            val notificationManager = context.getSystemService<NotificationManager>()
            if (!notificationManager?.areNotificationsEnabled()!!) {
                log("Notifications disabled, skipping scheduling")
                return
            }

            // Schedule for 8 AM and 8 PM
            val currentTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
            val times = listOf(
                currentTime.with(LocalTime.of(8, 0)),
                currentTime.with(LocalTime.of(20, 0))
            )
            times.forEachIndexed { index, targetTime ->
                val adjustedTime = if (currentTime.isAfter(targetTime)) targetTime.plusDays(1) else targetTime
                val delay = Duration.between(currentTime, adjustedTime).toMillis()

                val workRequest = PeriodicWorkRequestBuilder<UnfinishedWatchNotificationWorker>(
                    repeatInterval = 1,
                    repeatIntervalTimeUnit = TimeUnit.DAYS
                )
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .addTag("unfinished_notification_$index")
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "${WORK_NAME}_$index",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
                log("Scheduled unfinished notification worker at $adjustedTime (delay: ${delay}ms)")
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val attempt = params.runAttemptCount + 1
        val currentTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
        log("Started at $currentTime, attempt $attempt of $MAX_RETRIES")

        if (attempt > MAX_RETRIES) {
            log("Max retries reached")
            return@withContext Result.failure(workDataOf("error" to "Max retries exceeded"))
        }

        try {
            val notificationManager = context.getSystemService<NotificationManager>()
            if (!notificationManager?.areNotificationsEnabled()!!) {
                log("Notifications disabled, retrying")
                return@withContext Result.retry()
            }

            notificationRepository.cleanOldNotifications()
            log("Cleaned old notifications")

            val success = processRandomUnfinishedEpisode()
            log("Completed at $currentTime, notification sent: $success")
            Result.success()
        } catch (e: Exception) {
            log("Error: ${e.javaClass.name}, message=${e.message}, stacktrace=${e.stackTraceToString()}")
            Result.failure(workDataOf("error" to e.message))
        }
    }

    private suspend fun processRandomUnfinishedEpisode(): Boolean {
        val (episode, remainingEpisodes) = try {
            animeEpisodeDetailRepository.getRandomCachedUnfinishedEpisode()
        } catch (e: Exception) {
            log("Failed to get random episode: ${e.message}")
            return false
        }

        if (episode == null) {
            log("No unfinished episodes found")
            return false
        }
        log("Found unfinished episode: ${episode.animeTitle}, malId=${episode.malId}, episode=${episode.number}, remaining=$remainingEpisodes")

        val animeDetail = try {
            animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(episode.malId)
        } catch (e: Exception) {
            log("Failed to get anime detail for malId=${episode.malId}: ${e.message}")
            return false
        }

        if (animeDetail == null || animeDetail.eps == null || episode.number >= animeDetail.eps) {
            log("Invalid anime detail or episode not unfinished for malId=${episode.malId}")
            return false
        }

        return sendNotificationForEpisode(episode, remainingEpisodes)
    }

    private suspend fun sendNotificationForEpisode(episode: EpisodeDetailComplement, remainingEpisodes: Int): Boolean {
        val accessId = "${episode.malId}||${episode.id}"
        if (notificationRepository.checkDuplicateNotification(accessId, "UnfinishedWatch")) {
            log("Duplicate notification for ${episode.animeTitle} (accessId=$accessId)")
            return false
        }

        val notification = Notification(
            accessId = accessId,
            imageUrl = episode.imageUrl ?: "",
            contentText = "Continue watching ${episode.animeTitle} Episode ${episode.number}? $remainingEpisodes episode${if (remainingEpisodes > 1) "s" else ""} left!",
            type = "UnfinishedWatch"
        )
        try {
            val savedId = notificationRepository.saveNotification(notification)
            log("Saved notification: ${episode.animeTitle} (accessId=$accessId, notificationId=$savedId)")
            notificationHandler.sendNotification(context, notification, accessId.hashCode())
            notificationRepository.markNotificationAsSent(savedId)
            log("Sent notification for ${episode.animeTitle} (accessId=$accessId)")
            return true
        } catch (e: Exception) {
            log("Failed to send notification for ${episode.animeTitle}: ${e.message}, stacktrace=${e.stackTraceToString()}")
            return false
        }
    }
}