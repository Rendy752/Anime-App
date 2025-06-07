package com.luminoverse.animevibe.utils.factories

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
    private val workerFactories: Map<String, @JvmSuppressWildcards Provider<ChildWorkerFactory>>
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        val factoryProvider = workerFactories[workerClassName]
        return try {
            factoryProvider?.get()?.create(appContext, workerParameters)
                ?: throw IllegalArgumentException("Unknown worker class: $workerClassName")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}