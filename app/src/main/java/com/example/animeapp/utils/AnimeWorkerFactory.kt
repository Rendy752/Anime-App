package com.example.animeapp.utils

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeWorkerFactory @Inject constructor(
    private val workerFactories: Map<String, @JvmSuppressWildcards ChildWorkerFactory>
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        Log.d("AnimeWorkerFactory", "Attempting to create worker: $workerClassName")
        val factory = workerFactories[workerClassName]
        if (factory != null) {
            Log.d("AnimeWorkerFactory", "Found factory for $workerClassName")
            return factory.create(appContext, workerParameters)
        }

        Log.w(
            "AnimeWorkerFactory",
            "No factory found for $workerClassName, falling back to reflection"
        )
        return try {
            val workerClass =
                Class.forName(workerClassName).asSubclass(ListenableWorker::class.java)
            val constructor = workerClass.getDeclaredConstructor(
                Context::class.java,
                WorkerParameters::class.java
            )
            constructor.newInstance(appContext, workerParameters)
        } catch (e: Exception) {
            Log.e(
                "AnimeWorkerFactory",
                "Failed to create worker $workerClassName via reflection",
                e
            )
            null
        }
    }
}

interface ChildWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): ListenableWorker
}