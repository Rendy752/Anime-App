package com.luminoverse.animevibe.utils

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject
import javax.inject.Provider

interface ChildWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): ListenableWorker
}

class AnimeWorkerFactory @Inject constructor(
    private val notificationWorkerFactory: Provider<UnfinishedWatchNotificationWorker.Factory>,
    private val broadcastNotificationWorkerFactory: Provider<BroadcastNotificationWorker.Factory>
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
            else -> null
        }
    }
}