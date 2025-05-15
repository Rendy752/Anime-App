package com.luminoverse.animevibe.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.luminoverse.animevibe.models.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class NetworkStateMonitor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _downstreamSpeed = MutableLiveData<Int>()

    private val _networkStatus = MutableLiveData<NetworkStatus>()
    val networkStatus: LiveData<NetworkStatus> = _networkStatus

    private val handler = Handler(Looper.getMainLooper())
    private val connectivityCheckDelay = 1000L
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available: $network")
            postDelayedConnectivityCheck()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost: $network")
            postDelayedConnectivityCheck()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            Log.d(TAG, "Network capabilities changed: $networkCapabilities")
            _downstreamSpeed.postValue(networkCapabilities.linkDownstreamBandwidthKbps)
            postDelayedConnectivityCheck()
        }
    }

    private fun postDelayedConnectivityCheck() {
        handler.removeCallbacks(connectivityCheckRunnable)
        handler.postDelayed(connectivityCheckRunnable, connectivityCheckDelay)
    }

    private val connectivityCheckRunnable = Runnable {
        checkConnectivityInternal()
    }

    private fun checkConnectivityInternal() {
        if (ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_NETWORK_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Missing ACCESS_NETWORK_STATE permission")
            _isConnected.postValue(false)
            _downstreamSpeed.postValue(0)
            _networkStatus.postValue(
                NetworkStatus(
                    icon = Icons.Filled.Error,
                    label = "Permission Denied",
                    iconColor = Color.Red
                )
            )
            return
        }

        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }

        val hasNetwork = capabilities != null
        val hasInternetCapability =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        coroutineScope.launch {
            val isInternetAvailable = if (hasNetwork && hasInternetCapability) {
                withContext(Dispatchers.IO) { checkInternetAccess() }
            } else {
                false
            }

            Log.d(
                TAG,
                "Internet available: $isInternetAvailable, hasNetwork: $hasNetwork, hasInternetCapability: $hasInternetCapability"
            )
            _isConnected.postValue(isInternetAvailable)
            _downstreamSpeed.postValue(capabilities?.linkDownstreamBandwidthKbps ?: 0)
            updateNetworkStatus(capabilities, isInternetAvailable)
        }
    }

    /**
     * Checks actual internet connectivity by attempting HTTP connections to reliable endpoints.
     * Must be called on a background thread.
     * Returns true only if an HTTP connection is successful and not redirected to a carrier page.
     */
    private fun checkInternetAccess(): Boolean {
        // Primary check: HTTP to google.com
        try {
            val url = URL("https://www.google.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "HEAD"
            connection.connect()
            val responseCode = connection.responseCode
            val finalUrl = connection.url.toString()
            val locationHeader = connection.getHeaderField("Location") ?: ""
            connection.disconnect()
            if (responseCode in 200..299 && finalUrl.contains("google.com") && !locationHeader.contains("login")) {
                Log.d(TAG, "HTTP check to google.com successful: code=$responseCode, finalUrl=$finalUrl")
                return true
            } else {
                Log.w(TAG, "HTTP check to google.com failed: code=$responseCode, finalUrl=$finalUrl, location=$locationHeader")
            }
        } catch (e: IOException) {
            Log.w(TAG, "HTTP check to google.com failed: ${e.message}")
        }

        // Secondary check: HTTP to example.com
        try {
            val url = URL("https://example.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "HEAD"
            connection.connect()
            val responseCode = connection.responseCode
            val finalUrl = connection.url.toString()
            val locationHeader = connection.getHeaderField("Location") ?: ""
            connection.disconnect()
            if (responseCode in 200..299 && finalUrl.contains("example.com") && !locationHeader.contains("login")) {
                Log.d(TAG, "HTTP check to example.com successful: code=$responseCode, finalUrl=$finalUrl")
                return true
            } else {
                Log.w(TAG, "HTTP check to example.com failed: code=$responseCode, finalUrl=$finalUrl, location=$locationHeader")
            }
        } catch (e: IOException) {
            Log.w(TAG, "HTTP check to example.com failed: ${e.message}")
        }

        Log.w(TAG, "All internet checks failed")
        return false
    }

    private fun updateNetworkStatus(
        capabilities: NetworkCapabilities?,
        isInternetAvailable: Boolean
    ) {
        var icon: ImageVector = Icons.Filled.WifiOff
        var iconColor: Color = Color.Gray
        var label = "Offline"
        val speed = capabilities?.linkDownstreamBandwidthKbps ?: 0

        val isAirplaneModeOn = Settings.Global.getInt(
            appContext.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0

        if (isAirplaneModeOn) {
            icon = Icons.Filled.AirplanemodeActive
            iconColor = Color.Gray
            label = "Airplane Mode"
            _isConnected.postValue(false)
            _networkStatus.postValue(NetworkStatus(icon, label, iconColor))
            Log.d(TAG, "Network status: Airplane Mode, isConnected: false")
            return
        }

        val telephonyManager =
            appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        val isCellularDataEnabled =
            telephonyManager?.let {
                it.dataState == TelephonyManager.DATA_CONNECTED || it.dataState == TelephonyManager.DATA_CONNECTING
            } == true

        when {
            capabilities == null -> {
                icon = Icons.Filled.WifiOff
                iconColor = Color.Red
                label = "No Network"
                _isConnected.postValue(false)
            }
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> {
                icon = Icons.Filled.WifiOff
                iconColor = Color.Red
                label = "No Internet"
                _isConnected.postValue(false)
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && !isCellularDataEnabled -> {
                icon = Icons.Filled.SignalCellularOff
                iconColor = Color.Gray
                label = "Cellular Off"
                _isConnected.postValue(false)
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && isCellularDataEnabled && !isInternetAvailable -> {
                icon = Icons.Filled.SignalCellularConnectedNoInternet4Bar
                iconColor = Color.Yellow
                label = "No Data Quota"
                _isConnected.postValue(false)
            }
            else -> {
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
                    icon = Icons.Filled.Wifi
                    iconColor = Color.Green
                    label = "Connected"
                }
                _isConnected.postValue(true)
            }
        }
        _networkStatus.postValue(NetworkStatus(icon, label, iconColor))
        Log.d(TAG, "Network status: $label, isConnected: ${_isConnected.value}")
    }

    fun startMonitoring() {
        try {
            val builder = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
            checkConnectivityInternal()
            Log.d(TAG, "Started network monitoring")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start monitoring: ${e.message}")
            _networkStatus.postValue(
                NetworkStatus(
                    icon = Icons.Filled.Error,
                    label = "Monitoring Error",
                    iconColor = Color.Red
                )
            )
        }
    }

    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            handler.removeCallbacks(connectivityCheckRunnable)
            Log.d(TAG, "Stopped network monitoring")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping monitoring: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "NetworkStateMonitor"
    }
}