package com.luminoverse.animevibe.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.provider.Settings
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.models.NetworkStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStateMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkDataSource: NetworkDataSource
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkStatus = MutableStateFlow(
        NetworkStatus(
            icon = Icons.Filled.Wifi,
            label = "Connected",
            iconColor = Color.Green,
            isConnected = true
        )
    )
    val networkStatus = _networkStatus.asStateFlow()

    private val monitorScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitoringJob: Job? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            triggerStatusUpdate()
        }

        override fun onLost(network: Network) {
            triggerStatusUpdate()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            triggerStatusUpdate()
        }
    }

    private fun triggerStatusUpdate() {
        monitoringJob?.cancel()
        monitoringJob = monitorScope.launch {
            delay(500)
            checkAndUpdateNetworkStatus()
        }
    }

    private suspend fun checkAndUpdateNetworkStatus() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }

        val isAirplaneModeOn = Settings.Global.getInt(
            context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0

        if (isAirplaneModeOn) {
            _networkStatus.value = NetworkStatus(
                icon = Icons.Filled.AirplanemodeActive,
                label = "Airplane Mode",
                iconColor = Color.Gray,
                isConnected = false
            )
            return
        }

        if (activeNetwork == null || capabilities == null || !capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )
        ) {
            _networkStatus.value = NetworkStatus(
                icon = Icons.Filled.SignalWifiConnectedNoInternet4,
                label = "No Internet",
                iconColor = Color.Red,
                isConnected = false
            )
            return
        }

        val measuredSpeedKbps = measureDownloadSpeed()
        updateStatusWithMeasuredSpeed(capabilities, measuredSpeedKbps)
    }

    private suspend fun measureDownloadSpeed(): Int {
        val testUrl =
            "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_74x24dp.png"
        var speedKbps = 0
        try {
            val (bytes, durationMs) = networkDataSource.downloadFileAndMeasureTime(testUrl)
            if (durationMs > 0 && bytes > 0) {
                speedKbps = ((bytes * 8L * 1000L) / (durationMs * 1024L)).toInt()
                Log.d(TAG, "Speed test success: $speedKbps Kbps")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Speed test failed", e)
        }
        return speedKbps
    }

    private fun updateStatusWithMeasuredSpeed(
        capabilities: NetworkCapabilities,
        speed: Int
    ) {
        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val (icon: ImageVector, color: Color) = when {
            speed <= 0 && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> {
                (if (isWifi) Icons.Filled.Wifi else Icons.Filled.SignalCellularAlt) to Color.Green
            }

            speed in 1..100 -> {
                (if (isWifi) Icons.Filled.Wifi1Bar else Icons.Filled.SignalCellularAlt1Bar) to Color.Red
            }

            speed in 101..1500 -> {
                (if (isWifi) Icons.Filled.Wifi2Bar else Icons.Filled.SignalCellularAlt2Bar) to Color.Yellow
            }

            speed > 1500 -> {
                (if (isWifi) Icons.Filled.Wifi else Icons.Filled.SignalCellularAlt) to Color.Green
            }

            else -> {
                (Icons.Filled.SignalWifiConnectedNoInternet4) to Color.Red
            }
        }

        val isConnected =
            speed > 0 || capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        _networkStatus.value = NetworkStatus(
            icon = icon,
            label = if (speed > 0) "$speed Kbps" else if (isConnected) "Connected" else "No Internet",
            iconColor = color,
            isConnected = isConnected
        )
    }

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        monitorScope.launch {
            checkAndUpdateNetworkStatus()
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing network permissions to start monitoring", e)
            _networkStatus.value = NetworkStatus(
                icon = Icons.Filled.Error,
                label = "Permission Denied",
                iconColor = Color.Red,
                isConnected = false
            )
        }
    }

    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            monitoringJob?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping monitoring", e)
        }
    }

    companion object {
        private const val TAG = "NetworkStateMonitor"
    }
}
