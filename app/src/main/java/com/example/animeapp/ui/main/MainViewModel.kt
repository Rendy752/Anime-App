package com.example.animeapp.ui.main

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Stable
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.NetworkStatus
import com.example.animeapp.models.networkStatusPlaceholder
import com.example.animeapp.ui.theme.ColorStyle
import com.example.animeapp.ui.theme.ContrastMode
import com.example.animeapp.utils.NetworkStateMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
data class MainState(
    val isDarkMode: Boolean = false,
    val contrastMode: ContrastMode = ContrastMode.Normal,
    val colorStyle: ColorStyle = ColorStyle.Default,
    val notificationEnabled: Boolean = false,
    val showQuitDialog: Boolean = false,
    val isConnected: Boolean = true,
    val networkStatus: NetworkStatus = networkStatusPlaceholder,
    val isShowIdleDialog: Boolean = false,
    val isLandscape: Boolean = false
)

sealed class MainAction {
    data class SetDarkMode(val isDark: Boolean) : MainAction()
    data class SetContrastMode(val contrastMode: ContrastMode) : MainAction()
    data class SetColorStyle(val colorStyle: ColorStyle) : MainAction()
    data class SetNotificationEnabled(val enabled: Boolean) : MainAction()
    data class SetShowQuitDialog(val show: Boolean) : MainAction()
    data class SetIsConnected(val connected: Boolean) : MainAction()
    data class SetNetworkStatus(val status: NetworkStatus) : MainAction()
    data class SetIsShowIdleDialog(val show: Boolean) : MainAction()
    data object CheckNotificationPermission : MainAction()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val networkStateMonitor: NetworkStateMonitor
) : AndroidViewModel(application) {

    private val themePrefs = application.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    private val settingsPrefs =
        application.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        MainState(
            isDarkMode = themePrefs.getBoolean("is_dark_mode", false),
            contrastMode = themePrefs.getString("contrast_mode", ContrastMode.Normal.name)
                ?.let { ContrastMode.valueOf(it) } ?: ContrastMode.Normal,
            colorStyle = themePrefs.getString("color_style", ColorStyle.Default.name)
                ?.let { ColorStyle.valueOf(it) } ?: ColorStyle.Default,
            notificationEnabled = settingsPrefs.getBoolean("notifications_enabled", false)
        )
    )

    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        startNetworkMonitoring()
        println("MainViewModel initialized with notificationEnabled: ${_state.value.notificationEnabled}")
        checkNotificationPermission()
    }

    fun onAction(action: MainAction) {
        when (action) {
            is MainAction.SetDarkMode -> setDarkMode(action.isDark)
            is MainAction.SetContrastMode -> setContrastMode(action.contrastMode)
            is MainAction.SetColorStyle -> setColorStyle(action.colorStyle)
            is MainAction.SetNotificationEnabled -> setNotificationEnabled(action.enabled)
            is MainAction.SetShowQuitDialog -> setShowQuitDialog(action.show)
            is MainAction.SetIsConnected -> setIsConnected(action.connected)
            is MainAction.SetNetworkStatus -> setNetworkStatus(action.status)
            is MainAction.SetIsShowIdleDialog -> setIsShowIdleDialog(action.show)
            is MainAction.CheckNotificationPermission -> checkNotificationPermission()
        }
    }

    fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                getApplication(),
                "android.permission.POST_NOTIFICATIONS"
            )
            val isGranted = permissionStatus == PackageManager.PERMISSION_GRANTED
            println("POST_NOTIFICATIONS permission status: permissionStatus=$permissionStatus, isGranted=$isGranted")
            if (isGranted != _state.value.notificationEnabled) {
                println("Syncing notificationEnabled with permission: newValue=$isGranted")
                setNotificationEnabled(isGranted)
            }
        } else {
            println("POST_NOTIFICATIONS not required (pre-TIRAMISU), setting notificationEnabled=true")
            if (!_state.value.notificationEnabled) {
                setNotificationEnabled(true)
            }
        }
    }

    private fun setDarkMode(isDark: Boolean) {
        _state.update { it.copy(isDarkMode = isDark) }
        themePrefs.edit { putBoolean("is_dark_mode", isDark) }
    }

    private fun setContrastMode(contrastMode: ContrastMode) {
        _state.update { it.copy(contrastMode = contrastMode) }
        themePrefs.edit { putString("contrast_mode", contrastMode.name) }
    }

    private fun setColorStyle(colorStyle: ColorStyle) {
        _state.update { it.copy(colorStyle = colorStyle) }
        themePrefs.edit { putString("color_style", colorStyle.name) }
    }

    private fun setNotificationEnabled(enabled: Boolean) {
        println("setNotificationEnabled called with enabled=$enabled")
        _state.update { it.copy(notificationEnabled = enabled) }
        settingsPrefs.edit { putBoolean("notifications_enabled", enabled) }
        println("Updated notificationEnabled state to: ${_state.value.notificationEnabled}")
        val persisted = settingsPrefs.getBoolean("notifications_enabled", true)
        println("Persisted notifications_enabled in SharedPreferences: $persisted")
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
                onAction(MainAction.SetIsConnected(isNetworkAvailable))
            }
        }
        networkStateMonitor.networkStatus.observeForever { status ->
            viewModelScope.launch {
                onAction(MainAction.SetNetworkStatus(status))
            }
        }
    }

    public override fun onCleared() {
        super.onCleared()
        networkStateMonitor.stopMonitoring()
    }
}