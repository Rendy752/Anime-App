package com.luminoverse.animevibe.utils.debug

import android.content.Context
import com.luminoverse.animevibe.models.Notification
import com.luminoverse.animevibe.models.animeDetailPlaceholder
import com.luminoverse.animevibe.models.episodeDetailComplementPlaceholder
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.repository.NotificationRepository
import com.luminoverse.animevibe.utils.handlers.NotificationHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationDebugUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationHandler: NotificationHandler,
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend fun sendDebugNotification() {
        val broadcastNotification = Notification(
            accessId = animeDetailPlaceholder.mal_id.toString(),
            imageUrl = animeDetailPlaceholder.images.webp.large_image_url ?: "",
            contentText = "This is a debug broadcast notification!",
            type = "Broadcast"
        )
        try {
            val broadcastId = notificationRepository.saveNotification(broadcastNotification)
            log("Saved debug broadcast notification: id=$broadcastId, accessId=${broadcastNotification.accessId}")
            notificationHandler.sendNotification(context, broadcastNotification, 1)
            notificationRepository.markNotificationAsSent(broadcastId)
            log("Debug broadcast notification sent for ${animeDetailPlaceholder.title} (malId: ${animeDetailPlaceholder.mal_id}, id=$broadcastId)")
        } catch (e: Exception) {
            log("Failed to send debug broadcast notification: ${e.message}")
        }

        val episode = episodeDetailComplementPlaceholder.copy(
            id = "debug-ep-1",
            malId = animeDetailPlaceholder.mal_id,
            animeTitle = animeDetailPlaceholder.title,
            episodeTitle = "Episode 1",
            imageUrl = animeDetailPlaceholder.images.webp.large_image_url ?: "",
            number = 1,
            lastWatched = "2025-05-23"
        )
        animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(episode)
        val unfinishedNotification = Notification(
            accessId = "${episode.malId}||${episode.id}",
            imageUrl = episode.imageUrl,
            contentText = "Continue watching ${episode.animeTitle}: Episode ${episode.number}",
            type = "UnfinishedAnime"
        )
        try {
            val unfinishedId = notificationRepository.saveNotification(unfinishedNotification)
            log("Saved debug unfinished notification: id=$unfinishedId, accessId=${unfinishedNotification.accessId}")
            notificationHandler.sendNotification(context, unfinishedNotification, unfinishedNotification.accessId.hashCode())
            notificationRepository.markNotificationAsSent(unfinishedId)
            log("Debug unfinished notification sent for ${episode.animeTitle} (accessId: ${episode.malId}||${episode.id}, id=$unfinishedId)")
        } catch (e: Exception) {
            log("Failed to send debug unfinished notification: ${e.message}")
        }
    }

    private fun log(message: String) {
        println("NotificationDebugUtil: $message")
    }
}