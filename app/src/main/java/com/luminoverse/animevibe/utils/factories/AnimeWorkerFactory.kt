package com.luminoverse.animevibe.utils.factories

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.luminoverse.animevibe.utils.workers.BroadcastNotificationWorker
import com.luminoverse.animevibe.utils.workers.NotificationDeliveryWorker
import com.luminoverse.animevibe.utils.workers.UnfinishedWatchNotificationWorker
import javax.inject.Inject
import javax.inject.Provider

interface ChildWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): ListenableWorker
}

class AnimeWorkerFactory @Inject constructor(
    private val notificationWorkerFactory: Provider<UnfinishedWatchNotificationWorker.Factory>,
    private val broadcastNotificationWorkerFactory: Provider<BroadcastNotificationWorker.Factory>,
    private val notificationDeliveryWorkerFactory: Provider<NotificationDeliveryWorker.Factory>
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            UnfinishedWatchNotificationWorker::class.java.name -> {
                notificationWorkerFactory.get().create(appContext, workerParameters)
            }

            BroadcastNotificationWorker::class.java.name -> {
                broadcastNotificationWorkerFactory.get().create(appContext, workerParameters)
            }

            NotificationDeliveryWorker::class.java.name -> {
                notificationDeliveryWorkerFactory.get().create(appContext, workerParameters)
            }

            else -> null
        }
    }
}