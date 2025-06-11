// UnfinishedWatchNotificationWorker.kt
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
        override fun create(
            appContext: Context,
            params: WorkerParameters
        ): UnfinishedWatchNotificationWorker
    }

    companion object {
        private fun log(message: String) {
            println("UnfinishedWatchNotificationWorker: $message")
        }

        const val UNIQUE_WORK_NAME = "anime_unfinished_notification_work"
        // Added forceReschedule parameter
        fun schedule(context: Context, forceReschedule: Boolean = false) { //
            val notificationManager = context.getSystemService<NotificationManager>() //
            if (!notificationManager?.areNotificationsEnabled()!!) { //
                log("Notifications disabled, skipping scheduling") //
                return //
            }

            val workManager = WorkManager.getInstance(context) //
            val currentTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta")) //

            val targetTimes = listOf( //
                LocalTime.of(8, 0), //
                LocalTime.of(20, 0) //
            )

            targetTimes.forEachIndexed { index, targetTime -> //
                val uniqueWorkName = "${UNIQUE_WORK_NAME}_$index" //

                if (!forceReschedule) { //
                    val workInfo = workManager.getWorkInfosForUniqueWork(uniqueWorkName).get() //
                    if (workInfo.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }) { //
                        log("Work $uniqueWorkName already scheduled, skipping") //
                        return@forEachIndexed //
                    }
                }

                var adjustedTargetTime = currentTime.with(targetTime) //
                if (currentTime.isAfter(adjustedTargetTime)) { //
                    adjustedTargetTime = adjustedTargetTime.plusDays(1) //
                }

                val delay = Duration.between(currentTime, adjustedTargetTime).toMillis() //

                val workRequest = PeriodicWorkRequestBuilder<UnfinishedWatchNotificationWorker>( //
                    repeatInterval = 1, //
                    repeatIntervalTimeUnit = TimeUnit.DAYS //
                )
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS) //
                    .setConstraints( //
                        Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED) //
                            .build() //
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES) //
                    .addTag("unfinished_notification_$index") //
                    .build() //

                workManager.enqueueUniquePeriodicWork( //
                    uniqueWorkName, //
                    ExistingPeriodicWorkPolicy.REPLACE, //
                    workRequest //
                )
                log("Scheduled unfinished notification worker for $targetTime at $adjustedTargetTime (delay: ${delay}ms, index=$index)") //
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) { //
        log("doWork started. Run attempt: ${params.runAttemptCount}") //
        try {
            val (episode, remainingEpisodes) = animeEpisodeDetailRepository.getRandomCachedUnfinishedEpisode() //

            if (episode == null) { //
                log("No unfinished episodes found. Returning success.") //
                return@withContext Result.success() //
            }

            val result = sendNotificationForEpisode(episode, remainingEpisodes) //
            if (result) { //
                log("Notification sent successfully for ${episode.animeTitle}.") //
                Result.success() //
            } else {
                val accessId = "${episode.malId}||${episode.id}" //
                if (notificationRepository.checkDuplicateNotification( //
                        accessId, //
                        "UnfinishedWatch" //
                    ) //
                ) { //
                    log("Notification was a duplicate for ${episode.animeTitle}. Returning success.") //
                    Result.success() //
                } else {
                    log("Notification failed for ${episode.animeTitle}. Attempting retry.") //
                    if (params.runAttemptCount < 5) { //
                        Result.retry() //
                    } else {
                        log("Max retries reached for ${episode.animeTitle}. Returning failure.") //
                        Result.failure() //
                    }
                }
            }
        } catch (e: Exception) { //
            log("Error in doWork: ${e.message}, stacktrace=${e.stackTraceToString()}") //
            if (params.runAttemptCount < 5) { //
                Result.retry() //
            } else {
                log("Max retries reached due to exception. Returning failure.") //
                Result.failure(workDataOf("error" to e.message)) //
            }
        }
    }

    private suspend fun sendNotificationForEpisode( //
        episode: EpisodeDetailComplement, //
        remainingEpisodes: Int //
    ): Boolean {
        val accessId = "${episode.malId}||${episode.id}" //
        if (notificationRepository.checkDuplicateNotification(accessId, "UnfinishedWatch")) { //
            log("Duplicate notification for ${episode.animeTitle} (accessId=$accessId)") //
            return false //
        }

        val notification = Notification( //
            accessId = accessId, //
            imageUrl = episode.imageUrl ?: "", //
            contentText = "Hey, left off watching ${episode.animeTitle} Episode ${episode.number}? You have $remainingEpisodes episode${if (remainingEpisodes > 1) "s" else ""} left to enjoy. Dive back in to see what happens next!", //
            type = "UnfinishedWatch" //
        )
        try {
            val savedId = notificationRepository.saveNotification(notification) //
            log("Saved notification: ${episode.animeTitle} (accessId=$accessId, notificationId=$savedId)") //
            notificationHandler.sendNotification(context, notification, accessId.hashCode()) //
            notificationRepository.markNotificationAsSent(savedId) //
            log("Sent notification for ${episode.animeTitle} (accessId=$accessId)") //
            return true //
        } catch (e: Exception) { //
            log("Failed to send notification for ${episode.animeTitle}: ${e.message}, stacktrace=${e.stackTraceToString()}") //
            return false //
        }
    }
}