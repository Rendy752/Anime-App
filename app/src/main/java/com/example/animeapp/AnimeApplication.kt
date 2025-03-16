package com.example.animeapp

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorManager
import com.chuckerteam.chucker.api.Chucker
import com.example.animeapp.BuildConfig.DEBUG
import com.example.animeapp.utils.ShakeDetector
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AnimeApplication : Application() {
    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) setupSensor()
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