package com.example.animeapp

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.work.Configuration
import com.chuckerteam.chucker.api.Chucker
import com.example.animeapp.utils.AnimeBroadcastNotificationWorker
import com.example.animeapp.utils.AnimeWorkerFactory
import com.example.animeapp.utils.MediaPlaybackService
import com.example.animeapp.utils.ShakeDetector
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AnimeApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: AnimeWorkerFactory

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    private var mediaPlaybackService: MediaPlaybackService? = null
    private var isServiceBound = false
    private var serviceConnection: ServiceConnection? = null

    override val workManagerConfiguration: Configuration
        get() {
            Log.d("AnimeApplication", "Providing WorkManager configuration with workerFactory")
            require(::workerFactory.isInitialized) { "workerFactory not initialized" }
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        Log.d("AnimeApplication", "onCreate: Initializing application, workerFactory initialized: ${::workerFactory.isInitialized}")
        if (BuildConfig.DEBUG) setupSensor()
        bindMediaService()
        AnimeBroadcastNotificationWorker.schedule(this)
    }

    fun bindMediaService() {
        if (!isServiceBound) {
            serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    Log.d("AnimeApplication", "MediaPlaybackService connected")
                    mediaPlaybackService = (service as MediaPlaybackService.MediaPlaybackBinder).getService()
                    isServiceBound = true
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    Log.w("AnimeApplication", "MediaPlaybackService disconnected")
                    mediaPlaybackService = null
                    isServiceBound = false
                }
            }
            val intent = Intent(this, MediaPlaybackService::class.java)
            try {
                bindService(intent, serviceConnection!!, BIND_AUTO_CREATE)
                Log.d("AnimeApplication", "Bound to MediaPlaybackService")
            } catch (e: Exception) {
                Log.e("AnimeApplication", "Failed to bind MediaPlaybackService", e)
            }
        }
    }

    fun unbindMediaService() {
        if (isServiceBound) {
            try {
                serviceConnection?.let { unbindService(it) }
                Log.d("AnimeApplication", "Unbound MediaPlaybackService")
            } catch (e: IllegalArgumentException) {
                Log.w("AnimeApplication", "Service already unbound", e)
            }
            mediaPlaybackService = null
            isServiceBound = false
            serviceConnection = null
        }
    }

    fun getMediaPlaybackService(): MediaPlaybackService? = mediaPlaybackService

    fun isMediaServiceBound(): Boolean = isServiceBound

    fun cleanupService() {
        mediaPlaybackService?.let { service ->
            if (!service.isForegroundService()) {
                Log.d("AnimeApplication", "Stopping MediaPlaybackService from AnimeApplication")
                service.stopService()
                unbindMediaService()
            } else {
                Log.d("AnimeApplication", "Keeping MediaPlaybackService alive due to foreground state")
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        cleanupService()
        if (BuildConfig.DEBUG) {
            sensorManager.unregisterListener(shakeDetector)
        }
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
}