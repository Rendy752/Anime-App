package com.luminoverse.animevibe.utils.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.luminoverse.animevibe.models.Broadcast
import com.luminoverse.animevibe.models.ImageUrl
import com.luminoverse.animevibe.models.Images
import com.luminoverse.animevibe.models.Notification
import com.luminoverse.animevibe.models.animeDetailPlaceholder
import com.luminoverse.animevibe.repository.NotificationRepository
import com.luminoverse.animevibe.utils.factories.ChildWorkerFactory
import com.luminoverse.animevibe.utils.handlers.NotificationHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DebugNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val workerScheduler: WorkerScheduler,
    private val notificationHandler: NotificationHandler,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory : ChildWorkerFactory {
        override fun create(
            appContext: Context,
            params: WorkerParameters
        ): DebugNotificationWorker
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("DebugNotificationWorker", "Worker started. Preparing to send debug notification.")

        try {
            val dummyAnime = animeDetailPlaceholder.copy(
                mal_id = 21,
                title = "One Piece (Debug)",
                images = Images(
                    webp = ImageUrl(
                        image_url = "https://cdn.myanimelist.net/images/anime/1244/138851.webp",
                        small_image_url = "https://cdn.myanimelist.net/images/anime/1244/138851t.webp",
                        medium_image_url = "https://cdn.myanimelist.net/images/anime/1244/138851m.webp",
                        large_image_url = "https://cdn.myanimelist.net/images/anime/1244/138851.webp",
                        maximum_image_url = "https://cdn.myanimelist.net/images/anime/1244/138851.webp"
                    ),
                    jpg = ImageUrl(
                        image_url = "https://cdn.myanimelist.net/images/anime/1244/138851.jpg",
                        small_image_url = "https://cdn.myanimelist.net/images/anime/1244/138851t.jpg",
                        medium_image_url = "https://cdn.myanimelist.net/images/anime/1244/138851m.jpg",
                        large_image_url = "https://cdn.myanimelist.net/images/anime/1244/138851.jpg",
                        maximum_image_url = "https://cdn.myanimelist.net/images/anime/1244/138851.jpg"
                    )
                ),
                airing = true,
                broadcast = Broadcast(
                    day = "Sundays",
                    time = "09:30",
                    timezone = "Asia/Tokyo",
                    string = "Sundays at 09:30 (JST)"
                )
            )

            workerScheduler.sendBroadcastNotification(dummyAnime)

            Log.d(
                "DebugNotificationWorker",
                "Successfully triggered sendBroadcastNotification for animeId 21."
            )

            val unfinishedAccessId = "53447||to-be-hero-x-19591?ep=138072"
            val dummyUnfinishedNotification = Notification(
                accessId = unfinishedAccessId,
                imageUrl = "https://cdn.myanimelist.net/images/anime/1232/148474l.webp",
                contentText = "Hey, left off watching Tu Bian Yingxiong X Episode 8? You have 2 episodes left to enjoy. Dive back in to see what happens next!",
                type = "UnfinishedWatch"
            )
            val savedId = notificationRepository.saveNotification(dummyUnfinishedNotification)
            notificationHandler.sendNotification(context, dummyUnfinishedNotification, savedId.toInt())
            Log.d("DebugNotificationWorker", "Successfully triggered Unfinished Watch notification for accessId $unfinishedAccessId.")

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e("DebugNotificationWorker", "Error during debug worker execution: ${e.message}", e)
            return@withContext Result.failure()
        }
    }
}