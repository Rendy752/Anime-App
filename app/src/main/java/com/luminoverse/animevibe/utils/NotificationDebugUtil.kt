package com.luminoverse.animevibe.utils

import android.content.Context
import com.luminoverse.animevibe.models.animeDetailPlaceholder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationDebugUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationHandler: NotificationHandler
) {
    suspend fun sendDebugNotification() {
        notificationHandler.sendNotification(
            context = context,
            type = NotificationType.Broadcast(
                malId = animeDetailPlaceholder.mal_id,
                title = animeDetailPlaceholder.title,
                imageUrl = animeDetailPlaceholder.images.webp.large_image_url
            )
        )
        println("NotificationDebugUtil: Debug notification sent for ${animeDetailPlaceholder.title} (malId: ${animeDetailPlaceholder.mal_id})")
    }
}