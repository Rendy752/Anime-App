package com.luminoverse.animevibe

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.chuckerteam.chucker.api.Chucker
import com.luminoverse.animevibe.utils.factories.AnimeWorkerFactory
import com.luminoverse.animevibe.utils.workers.BroadcastNotificationWorker
import com.luminoverse.animevibe.utils.media.MediaPlaybackAction
import com.luminoverse.animevibe.utils.media.MediaPlaybackService
import com.luminoverse.animevibe.utils.debug.NotificationDebugUtil
import com.luminoverse.animevibe.utils.handlers.NotificationHandler
import com.luminoverse.animevibe.utils.shake.ShakeDetector
import com.luminoverse.animevibe.utils.workers.UnfinishedWatchNotificationWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AnimeApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: AnimeWorkerFactory

    @Inject
    lateinit var notificationDebugUtil: NotificationDebugUtil

    @Inject
    lateinit var notificationHandler: NotificationHandler

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    private var mediaPlaybackService: MediaPlaybackService? = null
    private var isServiceBound = false
    private var serviceConnection: ServiceConnection? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override val workManagerConfiguration: Configuration
        get() {
            Log.d("AnimeApplication", "Providing WorkManager configuration")
            require(::workerFactory.isInitialized) { "workerFactory not initialized" }
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        notificationHandler.createNotificationChannel(this)

        BroadcastNotificationWorker.schedule(this)
        UnfinishedWatchNotificationWorker.schedule(this)

        if (BuildConfig.DEBUG) {
            setupSensor()
        }

        bindMediaService()
    }

    // Debug method to cancel all work (use with caution)
    fun cancelAllWork() {
        Log.d("AnimeApplication", "Cancelling all WorkManager work")
        WorkManager.getInstance(this).cancelAllWork()
    }

    fun bindMediaService() {
        if (!isServiceBound) {
            serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    Log.d("AnimeApplication", "MediaPlaybackService connected")
                    mediaPlaybackService =
                        (service as MediaPlaybackService.MediaPlaybackBinder).getService()
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
            coroutineScope.launch {
                service.dispatch(MediaPlaybackAction.QueryForegroundStatus)
                service.state.collectLatest { state ->
                    if (!state.isForeground) {
                        Log.d("AnimeApplication", "Stopping MediaPlaybackService")
                        service.dispatch(MediaPlaybackAction.StopService)
                        unbindMediaService()
                    } else {
                        Log.d("AnimeApplication", "Keeping MediaPlaybackService alive")
                    }
                    this@launch.cancel()
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        cleanupService()
        if (BuildConfig.DEBUG) {
            sensorManager.unregisterListener(shakeDetector)
        }
        coroutineScope.cancel()
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