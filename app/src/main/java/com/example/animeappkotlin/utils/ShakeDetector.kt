package com.example.animeappkotlin.utils

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.abs

class ShakeDetector(private val onShakeListener: () -> Unit) : SensorEventListener {

    private var lastUpdate: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    override fun onSensorChanged(event: SensorEvent) {
        val curTime = System.currentTimeMillis()
        if (curTime - lastUpdate > 100) {
            val diffTime = curTime - lastUpdate
            lastUpdate = curTime
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val speed = abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000
            if (speed > SHAKE_THRESHOLD) {
                onShakeListener()
            }
            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    companion object {
        private const val SHAKE_THRESHOLD = 800
    }
}