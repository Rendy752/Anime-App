package com.example.animeapp

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.IBinder
import com.chuckerteam.chucker.api.Chucker
import com.example.animeapp.BuildConfig.DEBUG
import com.example.animeapp.utils.MediaPlaybackService
import com.example.animeapp.utils.ShakeDetector
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AnimeApplication : Application() {
    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    private var mediaPlaybackService: MediaPlaybackService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mediaPlaybackService = (service as MediaPlaybackService.MediaPlaybackBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaPlaybackService = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) setupSensor()
        bindService(
            Intent(this, MediaPlaybackService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )
    }

    fun getMediaPlaybackService(): MediaPlaybackService? = mediaPlaybackService

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