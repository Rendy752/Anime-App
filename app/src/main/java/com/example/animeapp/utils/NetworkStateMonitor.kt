package com.example.animeapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.animeapp.models.NetworkStatus

class NetworkStateMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _downstreamSpeed = MutableLiveData<Int>()

    private val _networkStatus = MutableLiveData<NetworkStatus>()
    val networkStatus: LiveData<NetworkStatus> = _networkStatus

    private val handler = Handler(Looper.getMainLooper())
    private val connectivityCheckDelay = 500L

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            postDelayedConnectivityCheck()
        }

        override fun onLost(network: Network) {
            postDelayedConnectivityCheck()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            _downstreamSpeed.postValue(networkCapabilities.linkDownstreamBandwidthKbps)
            updateNetworkStatus(networkCapabilities.linkDownstreamBandwidthKbps, context)
            postDelayedConnectivityCheck()
        }
    }

    private fun postDelayedConnectivityCheck() {
        handler.removeCallbacks(connectivityCheckRunnable)
        handler.postDelayed(connectivityCheckRunnable, connectivityCheckDelay)
    }

    private val connectivityCheckRunnable = Runnable {
        checkConnectivityInternal(context)
    }

    private fun checkConnectivityInternal(context: Context) {
        val activeNetwork = connectivityManager.activeNetwork ?: run {
            _isConnected.postValue(false)
            _downstreamSpeed.postValue(0)
            updateNetworkStatus(0, context)
            return
        }

        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: run {
            _isConnected.postValue(false)
            _downstreamSpeed.postValue(0)
            updateNetworkStatus(0, context)
            return
        }

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        _isConnected.postValue(hasInternet)
        _downstreamSpeed.postValue(capabilities.linkDownstreamBandwidthKbps)
        updateNetworkStatus(capabilities.linkDownstreamBandwidthKbps, context)
    }

    private fun updateNetworkStatus(speed: Int, context: Context) {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val networkInfo = connectivityManager.activeNetworkInfo

        val isAirplaneModeOn = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0

        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        val isCellularDataEnabled =
            if (telephonyManager != null) {
                telephonyManager.dataState == TelephonyManager.DATA_CONNECTED || telephonyManager.dataState == TelephonyManager.DATA_CONNECTING
            } else {
                false
            }

        var icon: ImageVector = Icons.Filled.WifiOff
        var iconColor: Color = Color.Gray
        var label = "Offline"

        if (isAirplaneModeOn) {
            icon = Icons.Filled.AirplanemodeActive
            iconColor = Color.Gray
            label = "Airplane"
        } else if (activeNetwork == null || capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == false) {
            if (networkInfo?.type == ConnectivityManager.TYPE_MOBILE && !isCellularDataEnabled) {
                icon = Icons.Filled.SignalCellularOff
                iconColor = Color.Gray
                label = "Cellular Off"
            } else {
                icon = Icons.Filled.WifiOff
                iconColor = Color.Red
                label = "No Internet"
            }
        } else if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                when (speed) {
                    in 1..5000 -> {
                        icon = Icons.Filled.Wifi1Bar
                        iconColor = Color.Red
                        label = "$speed Kbps"
                    }

                    in 5001..10000 -> {
                        icon = Icons.Filled.Wifi2Bar
                        iconColor = Color.Yellow
                        label = "$speed Kbps"
                    }

                    else -> {
                        icon = Icons.Filled.Wifi
                        iconColor = Color.Green
                        label = "$speed Kbps"
                    }
                }
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                when (speed) {
                    in 1..5000 -> {
                        icon = Icons.Filled.SignalCellularAlt1Bar
                        iconColor = Color.Red
                        label = "$speed Kbps"
                    }

                    in 5001..10000 -> {
                        icon = Icons.Filled.SignalCellularAlt2Bar
                        iconColor = Color.Yellow
                        label = "$speed Kbps"
                    }

                    else -> {
                        icon = Icons.Filled.SignalCellularAlt
                        iconColor = Color.Green
                        label = "$speed Kbps"
                    }
                }
            } else {
                icon = Icons.Filled.WifiOff
                iconColor = Color.Green
                label = "Connected"
            }
        }
        _networkStatus.postValue(NetworkStatus(icon, label, iconColor))
    }

    fun startMonitoring(context: Context) {
        val builder = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback)

        checkConnectivityInternal(context)
    }

    fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        handler.removeCallbacks(connectivityCheckRunnable)
    }
}