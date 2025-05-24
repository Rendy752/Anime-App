package com.luminoverse.animevibe.utils

import android.app.NotificationManager
import android.content.Context
import androidx.work.*
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.Notification
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.repository.NotificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import java.time.ZonedDateTime
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

        private fun buildConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun schedule(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.areNotificationsEnabled()) {
                log("Notifications disabled, skipping scheduling")
                return
            }

            val workRequest = PeriodicWorkRequestBuilder<UnfinishedWatchNotificationWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(buildConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .addTag("unfinished_notification")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            log("Scheduled unfinished notification worker (every 6 hours)")
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

            val workRequest = OneTimeWorkRequestBuilder<UnfinishedWatchNotificationWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("unfinished_notification")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
            )
            log("Scheduled immediate unfinished notification")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val attempt = params.runAttemptCount + 1
        val currentTime = ZonedDateTime.now()
        log("Started at $currentTime, attempt $attempt of $MAX_RETRIES")

        if (attempt > MAX_RETRIES) {
            log("Max retries reached")
            return@withContext Result.failure()
        }

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.areNotificationsEnabled()) {
                log("Notifications disabled, skipping")
                return@withContext Result.success()
            }

            val channel = notificationManager.getNotificationChannel("anime_notifications")
            log("Channel status: importance=${channel?.importance}, enabled=${channel?.importance != NotificationManager.IMPORTANCE_NONE}")

            notificationRepository.cleanOldNotifications()
            log("Cleaned old notifications")

            val success = processLatestWatchedEpisode()

            log("Completed successfully at $currentTime, notification sent: $success")
            Result.success()
        } catch (_: UnknownHostException) {
            log("Network error, retrying")
            Result.retry()
        } catch (e: Exception) {
            log("Error: ${e.message}, stacktrace=${e.stackTraceToString()}")
            Result.failure()
        }
    }

    private suspend fun processLatestWatchedEpisode(): Boolean {
        val episode = animeEpisodeDetailRepository.getCachedLatestWatchedEpisodeDetailComplement()
        if (episode == null) {
            log("No latest watched episode in cache")
            return false
        }
        log("Found episode: ${episode.animeTitle}, malId=${episode.malId}")

        val animeDetail = animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(episode.malId)
        if (animeDetail == null) {
            log("No anime detail for malId=${episode.malId}")
            return false
        }
        log("Found anime detail: episodes=${animeDetail.eps}")

        if (animeDetail.eps != null && episode.number >= animeDetail.eps) {
            log("Episode ${episode.number} of ${episode.animeTitle} is not unfinished")
            return false
        }

        return sendNotificationForEpisode(episode)
    }

    private suspend fun sendNotificationForEpisode(episode: EpisodeDetailComplement): Boolean {
        val accessId = "${episode.malId}||${episode.id}"
        if (notificationRepository.checkDuplicateNotification(accessId, "UnfinishedAnime")) {
            log("Duplicate notification for ${episode.animeTitle} (accessId=$accessId)")
            return false
        }

        val notification = Notification(
            accessId = accessId,
            imageUrl = episode.imageUrl,
            contentText = "Continue watching ${episode.animeTitle}: Episode ${episode.number}",
            type = "UnfinishedAnime"
        )
        try {
            val savedId = notificationRepository.saveNotification(notification)
            log("Saved notification for ${episode.animeTitle} (accessId=$accessId, notificationId=$savedId)")

            notificationHandler.sendNotification(context, notification, accessId.hashCode())
            log("Sent notification for ${episode.animeTitle} (accessId=$accessId, id=${accessId.hashCode()})")

            notificationRepository.getPendingNotifications()
                .find { it.accessId == accessId && it.type == "UnfinishedAnime" && it.id == savedId }
                ?.let {
                    notificationRepository.markNotificationAsSent(it.id)
                    log("Marked notification as sent (id=${it.id})")
                } ?: log("No pending notification found for ${episode.animeTitle} (accessId=$accessId)")
            return true
        } catch (e: Exception) {
            log("Failed to save notification for ${episode.animeTitle} (accessId=$accessId): ${e.message}, stacktrace=${e.stackTraceToString()}")
            return false
        }
    }
}