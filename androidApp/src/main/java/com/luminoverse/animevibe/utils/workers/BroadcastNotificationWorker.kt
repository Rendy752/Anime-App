package com.luminoverse.animevibe.utils.workers

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.work.*
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.repository.NotificationRepository
import com.luminoverse.animevibe.utils.factories.ChildWorkerFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

class BroadcastNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository,
    private val notificationRepository: NotificationRepository,
    private val workerScheduler: WorkerScheduler
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory : ChildWorkerFactory {
        override fun create(
            appContext: Context,
            params: WorkerParameters
        ): BroadcastNotificationWorker
    }

    companion object {
        private fun log(message: String) = println("BroadcastNotificationWorker: $message")
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        log("Daily worker started at ${ZonedDateTime.now()}")

        val notificationManager = applicationContext.getSystemService<NotificationManager>()
        if (notificationManager?.areNotificationsEnabled() != true) {
            log("Notifications disabled, finishing work.")
            return@withContext Result.success()
        }

        try {
            notificationRepository.cleanOldNotifications()
            val favoriteSchedules = fetchFavoriteAiringAnime()
            log("Fetched ${favoriteSchedules.size} favorite anime schedules from database")
            workerScheduler.processAndScheduleBroadcasts(favoriteSchedules)
            log("Daily worker completed successfully.")
            Result.success()
        } catch (e: Exception) {
            log("Error during daily worker execution: ${e.message}")
            return@withContext Result.failure()
        }
    }

    private suspend fun fetchFavoriteAiringAnime(): List<AnimeDetail> {
        val favoriteComplements = animeEpisodeDetailRepository.getAllFavoriteAnimeComplements()
        val favoriteAnimeDetails = mutableListOf<AnimeDetail>()
        for (complement in favoriteComplements) {
            val animeDetail = animeEpisodeDetailRepository.getCachedAnimeDetailById(complement.malId)
            if (animeDetail != null && animeDetail.airing) {
                favoriteAnimeDetails.add(animeDetail)
            }
        }
        return favoriteAnimeDetails
    }
}