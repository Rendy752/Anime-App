package com.luminoverse.animevibe

import android.app.Application
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.work.Configuration
import com.chuckerteam.chucker.api.Chucker
import com.luminoverse.animevibe.utils.factories.AnimeWorkerFactory
import com.luminoverse.animevibe.utils.media.MediaPlaybackAction
import com.luminoverse.animevibe.utils.media.MediaPlaybackService
import com.luminoverse.animevibe.utils.debug.NotificationDebugUtil
import com.luminoverse.animevibe.utils.handlers.NotificationHandler
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import com.luminoverse.animevibe.utils.shake.ShakeDetector
import com.luminoverse.animevibe.utils.workers.BroadcastNotificationWorker
import com.luminoverse.animevibe.utils.workers.UnfinishedWatchNotificationWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    @Inject
    lateinit var hlsPlayerUtils: HlsPlayerUtils

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    private var mediaPlaybackService: MediaPlaybackService? = null
    private var isServiceBound = false
    private var serviceConnection: ServiceConnection? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .also { Log.d(TAG, "Providing WorkManager configuration") }
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")
        notificationHandler.createNotificationChannel(this)

        BroadcastNotificationWorker.schedule(this)
        UnfinishedWatchNotificationWorker.schedule(this)

        if (BuildConfig.DEBUG) {
            setupSensor()
        }

        Log.d(TAG, "HlsPlayerUtils instance: $hlsPlayerUtils")
        manageMediaService(true)
    }

    override fun onTerminate() {
        super.onTerminate()
        manageMediaService(false)
        if (BuildConfig.DEBUG) {
            sensorManager.unregisterListener(shakeDetector)
        }
        hlsPlayerUtils.dispatch(HlsPlayerAction.Release)
        coroutineScope.cancel()
    }

    fun cleanupService() {
        hlsPlayerUtils.dispatch(HlsPlayerAction.Release)
        mediaPlaybackService?.let { service ->
            Log.d(TAG, "Cleaning up MediaPlaybackService")
            service.dispatch(MediaPlaybackAction.StopService)
            manageMediaService(false)
        } ?: Log.d(TAG, "No MediaPlaybackService to clean up")
    }

    fun stopMediaServiceForWorker(): Boolean {
        mediaPlaybackService?.let { service ->
            coroutineScope.launch {
                val isForeground = try {
                    service.isForeground()
                } catch (_: Exception) {
                    val notificationManager =
                        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.activeNotifications.any { it.id == service.getNotificationId() }
                }
                if (!isForeground) {
                    Log.d(TAG, "Stopping MediaPlaybackService for worker")
                    cleanupService()
                } else {
                    Log.d(TAG, "MediaPlaybackService is in foreground, not stopping")
                }
            }
            return true
        }
        Log.d(TAG, "MediaPlaybackService not running, no need to stop")
        return false
    }

    fun restartMediaService() {
        if (!isServiceBound) {
            Log.d(TAG, "Restarting MediaPlaybackService")
            manageMediaService(true)
        } else {
            Log.d(TAG, "MediaPlaybackService already running")
        }
    }

    fun getMediaPlaybackService(): MediaPlaybackService? = mediaPlaybackService

    fun isMediaServiceBound(): Boolean = isServiceBound

    private fun manageMediaService(bind: Boolean) {
        if (bind && !isServiceBound) {
            serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    Log.d(TAG, "MediaPlaybackService connected")
                    mediaPlaybackService =
                        (service as MediaPlaybackService.MediaPlaybackBinder).getService()
                    isServiceBound = true
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    Log.w(TAG, "MediaPlaybackService disconnected")
                    mediaPlaybackService = null
                    isServiceBound = false
                }
            }
            val intent = Intent(this, MediaPlaybackService::class.java)
            try {
                bindService(intent, serviceConnection!!, BIND_AUTO_CREATE)
                Log.d(TAG, "Bound to MediaPlaybackService")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind MediaPlaybackService", e)
            }
        } else if (!bind && isServiceBound) {
            try {
                serviceConnection?.let { unbindService(it) }
                Log.d(TAG, "Unbound MediaPlaybackService")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Service already unbound", e)
            }
            mediaPlaybackService = null
            isServiceBound = false
            serviceConnection = null
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

    companion object {
        private const val TAG = "AnimeApplication"
    }
}