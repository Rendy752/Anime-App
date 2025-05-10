package com.example.animeapp.utils

import android.content.Context
import com.example.animeapp.models.animeDetailPlaceholder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationDebugUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun sendDebugNotification() {
        AnimeBroadcastNotificationWorker.createNotificationChannel(context)
        AnimeBroadcastNotificationWorker.sendNotification(
            context = context,
            malId = animeDetailPlaceholder.mal_id,
            title = animeDetailPlaceholder.title,
            imageUrl = animeDetailPlaceholder.images.webp.large_image_url
        )
    }
}