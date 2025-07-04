package com.luminoverse.animevibe.utils.workers

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
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

class UnfinishedWatchNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val notificationRepository: NotificationRepository,
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory : ChildWorkerFactory {
        override fun create(
            appContext: Context,
            params: WorkerParameters
        ): UnfinishedWatchNotificationWorker
    }

    companion object {
        private fun log(message: String) {
            println("UnfinishedWatchNotificationWorker: $message")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        log("doWork started. Run attempt: ${params.runAttemptCount}")

        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            log("Notifications disabled at system level, finishing work.")
            return@withContext Result.success()
        }

        val settingsPrefs = applicationContext.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val unfinishedEnabled = settingsPrefs.getBoolean("notifications_unfinished_enabled", true)
        if (!unfinishedEnabled) {
            log("Continue Watching reminders are disabled by the user in app settings. Skipping work.")
            return@withContext Result.success()
        }

        try {
            val (episode, remainingEpisodes) = animeEpisodeDetailRepository.getRandomCachedUnfinishedEpisode()

            if (episode == null) {
                log("No unfinished episodes found. Returning success.")
                return@withContext Result.success()
            }

            val result = sendNotificationForEpisode(episode, remainingEpisodes)
            if (result) {
                log("Notification sent successfully for ${episode.animeTitle}.")
                Result.success()
            } else {
                val accessId = "${episode.malId}||${episode.id}"
                if (notificationRepository.checkDuplicateNotification(
                        accessId,
                        "UnfinishedWatch"
                    )
                ) {
                    log("Notification was a duplicate for ${episode.animeTitle}. Returning success.")
                    Result.success()
                } else {
                    log("Notification failed for ${episode.animeTitle}. Attempting retry.")
                    if (params.runAttemptCount < 5) {
                        Result.retry()
                    } else {
                        log("Max retries reached for ${episode.animeTitle}. Returning failure.")
                        Result.failure()
                    }
                }
            }
        } catch (e: Exception) {
            log("Error in doWork: ${e.message}, stacktrace=${e.stackTraceToString()}")
            if (params.runAttemptCount < 5) {
                Result.retry()
            } else {
                log("Max retries reached due to exception. Returning failure.")
                Result.failure(workDataOf("error" to e.message))
            }
        }
    }

    private suspend fun sendNotificationForEpisode(
        episode: EpisodeDetailComplement,
        remainingEpisodes: Int
    ): Boolean {
        val accessId = "${episode.malId}||${episode.id}"
        if (notificationRepository.checkDuplicateNotification(accessId, "UnfinishedWatch")) {
            log("Duplicate notification for ${episode.animeTitle} (accessId=$accessId)")
            return false
        }

        val notification = Notification(
            accessId = accessId,
            imageUrl = episode.imageUrl ?: "",
            contentText = "Hey, left off watching ${episode.animeTitle} Episode ${episode.number}? You have $remainingEpisodes episode${if (remainingEpisodes > 1) "s" else ""} left to enjoy. Dive back in to see what happens next!",
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