package com.example.animeapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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

    init {
        checkConnectivity(context)
    }

    private fun checkConnectivity(context: Context) {
        val network = connectivityManager.activeNetwork ?: return
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return
        val isCurrentlyConnected = when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
        _isConnected.postValue(isCurrentlyConnected)

        _downstreamSpeed.postValue(activeNetwork.linkDownstreamBandwidthKbps)

        updateNetworkStatus(activeNetwork.linkDownstreamBandwidthKbps, context)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isConnected.postValue(true)
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return
            _downstreamSpeed.postValue(activeNetwork.linkDownstreamBandwidthKbps)
            updateNetworkStatus(activeNetwork.linkDownstreamBandwidthKbps, context)
        }

        override fun onLost(network: Network) {
            _isConnected.postValue(false)
            _downstreamSpeed.postValue(0)
            updateNetworkStatus(0, context)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            _downstreamSpeed.postValue(networkCapabilities.linkDownstreamBandwidthKbps)
            updateNetworkStatus(networkCapabilities.linkDownstreamBandwidthKbps, context)
        }
    }

    private fun updateNetworkStatus(speed: Int, context: Context) {
        val connectivityManager = connectivityManager
        val network = connectivityManager.activeNetwork
        val activeNetwork = connectivityManager.getNetworkCapabilities(network)
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
        } else if (networkInfo == null || !networkInfo.isConnected) {
            _networkStatus.postValue(
                NetworkStatus(Icons.Filled.SignalCellularOff, "No signal", Color.Gray)
            )
            return
        } else if (activeNetwork != null) {
             if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
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
            } else if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
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
                iconColor = Color.Red
                label = "Unknown"
            }
        } else {
            if (!isCellularDataEnabled) {
                icon = Icons.Filled.SignalCellularOff
                iconColor = Color.Gray
                label = "Cellular Off"
                _networkStatus.postValue(
                    NetworkStatus(
                        Icons.Filled.SignalCellularOff,
                        "Cellular Off",
                        iconColor
                    )
                )
            } else {
                icon = Icons.Filled.WifiOff
                iconColor = Color.Gray
                label = "Offline"
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

        checkConnectivity(context)
    }

    fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}