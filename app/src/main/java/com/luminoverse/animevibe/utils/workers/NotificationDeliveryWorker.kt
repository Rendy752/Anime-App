package com.luminoverse.animevibe.utils.workers

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.luminoverse.animevibe.models.Notification
import com.luminoverse.animevibe.repository.NotificationRepository
import com.luminoverse.animevibe.utils.factories.ChildWorkerFactory
import com.luminoverse.animevibe.utils.handlers.NotificationHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationDeliveryWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory : ChildWorkerFactory {
        override fun create(
            appContext: Context,
            params: WorkerParameters
        ): NotificationDeliveryWorker
    }

    companion object {
        private fun log(message: String) {
            println("NotificationDeliveryWorker: $message")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        log(
            "Started processing work for notification_id=${
                params.inputData.getLong(
                    "notification_id",
                    0
                )
            }"
        )
        try {
            val notificationManager = context.getSystemService<NotificationManager>()
            if (notificationManager?.areNotificationsEnabled() != true) {
                log("Notifications disabled, retrying")
                return@withContext Result.retry()
            }

            val notificationId = params.inputData.getLong("notification_id", 0)
            val accessId = params.inputData.getString("access_id")
                ?: return@withContext Result.failure().also { log("Missing access_id") }
            val contentText = params.inputData.getString("content_text")
                ?: return@withContext Result.failure().also { log("Missing content_text") }
            val imageUrl = params.inputData.getString("image_url")
            val type = params.inputData.getString("type")
                ?: return@withContext Result.failure().also { log("Missing type") }

            val notification = Notification(
                id = notificationId,
                accessId = accessId,
                imageUrl = imageUrl,
                contentText = contentText,
                type = type
            )

            notificationHandler.sendNotification(context, notification, accessId.hashCode())
            log("Sent notification for accessId=$accessId, id=$notificationId")

            notificationRepository.markNotificationAsSent(notificationId)
            log("Marked notification as sent (id=$notificationId)")

            Result.success()
        } catch (e: Exception) {
            log("Failed to send notification: ${e.message}, stacktrace=${e.stackTraceToString()}")
            if (params.runAttemptCount < 5) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to e.message))
            }
        }
    }
}