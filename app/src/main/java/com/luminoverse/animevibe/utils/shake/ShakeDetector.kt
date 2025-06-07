package com.luminoverse.animevibe.utils.shake

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.abs

class ShakeDetector(private val onShakeListener: () -> Unit) : SensorEventListener {

    private var lastUpdate: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var shakeCount = 0
    private var lastDirection = 0
    private var shaking = false

    override fun onSensorChanged(event: SensorEvent) {
        val curTime = System.currentTimeMillis()
        if (curTime - lastUpdate > 50) {
            val diffTime = curTime - lastUpdate
            lastUpdate = curTime
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val speed = abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000

            if (speed > SHAKE_THRESHOLD) {
                val deltaX = x - lastX

                if (abs(deltaX) > DIRECTION_THRESHOLD) {
                    val currentDirection = if (deltaX > 0) 1 else -1

                    if (shaking) {
                        if (currentDirection != lastDirection && lastDirection != 0) {
                            shakeCount++
                            lastDirection = currentDirection
                        }
                    } else {
                        shaking = true
                        lastDirection = currentDirection
                        shakeCount = 1
                    }

                    if (shakeCount >= REQUIRED_SHAKES) {
                        onShakeListener()
                        resetShake()
                    }
                }

            } else {
                if (shaking) {
                    resetShake()
                }
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }

    private fun resetShake() {
        shakeCount = 0
        lastDirection = 0
        shaking = false
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    companion object {
        private const val SHAKE_THRESHOLD = 300
        private const val DIRECTION_THRESHOLD = 1.0f
        private const val REQUIRED_SHAKES = 4
    }
}