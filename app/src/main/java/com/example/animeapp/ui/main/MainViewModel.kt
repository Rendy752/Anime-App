package com.example.animeapp.ui.main

import android.app.Application
import android.content.Context
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
import androidx.core.content.edit
import kotlinx.coroutines.flow.update

data class MainState(
    val isDarkMode: Boolean = false,
    val themeApplied: Boolean = false,
    val showQuitDialog: Boolean = false,
    val isConnected: Boolean = true,
    val networkStatus: NetworkStatus = networkStatusPlaceholder,
    val isShowIdleDialog: Boolean = false,
    val isLandscape: Boolean = false
)

sealed class MainAction {
    data class SetDarkMode(val isDark: Boolean) : MainAction()
    data class SetThemeApplied(val applied: Boolean) : MainAction()
    data class SetShowQuitDialog(val show: Boolean) : MainAction()
    data class SetIsConnected(val connected: Boolean) : MainAction()
    data class SetNetworkStatus(val status: NetworkStatus) : MainAction()
    data class SetIsShowIdleDialog(val show: Boolean) : MainAction()
}

@HiltViewModel
class MainViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val themePrefs = application.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        MainState(isDarkMode = themePrefs.getBoolean("is_dark_mode", false))
    )
    val state: StateFlow<MainState> = _state.asStateFlow()

    private val networkStateMonitor = NetworkStateMonitor(application)

    init {
        startNetworkMonitoring()
    }

    fun dispatch(action: MainAction) {
        when (action) {
            is MainAction.SetDarkMode -> setDarkMode(action.isDark)
            is MainAction.SetThemeApplied -> setThemeApplied(action.applied)
            is MainAction.SetShowQuitDialog -> setShowQuitDialog(action.show)
            is MainAction.SetIsConnected -> setIsConnected(action.connected)
            is MainAction.SetNetworkStatus -> setNetworkStatus(action.status)
            is MainAction.SetIsShowIdleDialog -> setIsShowIdleDialog(action.show)
        }
    }

    private fun setDarkMode(isDark: Boolean) {
        _state.update { it.copy(isDarkMode = isDark) }
        themePrefs.edit { putBoolean("is_dark_mode", isDark) }
    }

    private fun setThemeApplied(applied: Boolean) {
        _state.update { it.copy(themeApplied = applied) }
    }

    private fun setShowQuitDialog(show: Boolean) {
        _state.update { it.copy(showQuitDialog = show) }
    }

    private fun setIsConnected(connected: Boolean) {
        _state.update { it.copy(isConnected = connected) }
    }

    private fun setNetworkStatus(status: NetworkStatus) {
        _state.update { it.copy(networkStatus = status) }
    }

    private fun setIsShowIdleDialog(show: Boolean) {
        _state.update { it.copy(isShowIdleDialog = show) }
    }

    private fun startNetworkMonitoring() {
        networkStateMonitor.startMonitoring(getApplication())
        networkStateMonitor.isConnected.observeForever { isNetworkAvailable ->
            viewModelScope.launch {
                dispatch(MainAction.SetIsConnected(isNetworkAvailable))
            }
        }
        networkStateMonitor.networkStatus.observeForever { status ->
            viewModelScope.launch {
                dispatch(MainAction.SetNetworkStatus(status))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkStateMonitor.stopMonitoring()
    }
}