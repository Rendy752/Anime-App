package com.example.animeapp.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.NetworkStatus
import com.example.animeapp.models.networkStatusPlaceholder
import com.example.animeapp.utils.NetworkStateMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val _themeApplied = MutableStateFlow(false)
    val themeApplied: StateFlow<Boolean> = _themeApplied.asStateFlow()

    private val _showQuitDialog = MutableStateFlow(false)
    val showQuitDialog: StateFlow<Boolean> = _showQuitDialog.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _networkStatus = MutableStateFlow(networkStatusPlaceholder)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val networkStateMonitor = NetworkStateMonitor(application)

    init { startNetworkMonitoring() }

    private fun startNetworkMonitoring() {
        networkStateMonitor.startMonitoring(getApplication())
        networkStateMonitor.isConnected.observeForever { isNetworkAvailable ->
            viewModelScope.launch {
                _isConnected.value = isNetworkAvailable
            }
        }
        networkStateMonitor.networkStatus.observeForever { status ->
            viewModelScope.launch {
                _networkStatus.value = status
            }
        }
    }

    fun setThemeApplied(applied: Boolean) {
        _themeApplied.value = applied
    }

    fun setShowQuitDialog(show: Boolean) {
        _showQuitDialog.value = show
    }

    override fun onCleared() {
        super.onCleared()
        networkStateMonitor.stopMonitoring()
    }
}