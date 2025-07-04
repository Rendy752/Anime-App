package com.luminoverse.animevibe

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.chuckerteam.chucker.api.Chucker
import com.luminoverse.animevibe.android.BuildConfig
import com.luminoverse.animevibe.utils.factories.AnimeWorkerFactory
import com.luminoverse.animevibe.utils.debug.NotificationDebugUtil
import com.luminoverse.animevibe.utils.handlers.NotificationHandler
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import com.luminoverse.animevibe.utils.shake.ShakeDetector
import com.luminoverse.animevibe.utils.workers.WorkerScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class AnimeApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: AnimeWorkerFactory

    @Inject
    lateinit var workerScheduler: WorkerScheduler

    @Inject
    lateinit var notificationDebugUtil: NotificationDebugUtil

    @Inject
    lateinit var notificationHandler: NotificationHandler

    @Inject
    lateinit var hlsPlayerUtils: HlsPlayerUtils

    @Inject
    lateinit var imageLoaderProvider: Provider<ImageLoader>

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .also { Log.d(TAG, "Providing WorkManager configuration") }
            .build()

    override fun onCreate() {
        super.onCreate()

        notificationHandler.createNotificationChannel(this)
        scheduleBackgroundWorkers()

        if (BuildConfig.DEBUG) {
            setupSensor()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return imageLoaderProvider.get()
    }

    private fun scheduleBackgroundWorkers() {
        workerScheduler.scheduleBroadcastNotifications()
        workerScheduler.scheduleUnfinishedWatchNotifications()
    }

    private fun setupSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector { startActivity(Chucker.getLaunchIntent(this)) }
        sensorManager.registerListener(
            shakeDetector,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    companion object {
        private const val TAG = "AnimeApplication"
    }
}