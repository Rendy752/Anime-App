package com.luminoverse.animevibe.ui.main

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Stable
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminoverse.animevibe.models.NetworkStatus
import com.luminoverse.animevibe.models.networkStatusPlaceholder
import com.luminoverse.animevibe.ui.theme.ColorStyle
import com.luminoverse.animevibe.ui.theme.ContrastMode
import com.luminoverse.animevibe.utils.NetworkStateMonitor
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
    val isNotificationEnabled: Boolean = false,
    val isAutoPlayVideo: Boolean = true,
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
    data class SetAutoPlayVideo(val isAutoPlayVideo: Boolean) : MainAction()
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
            isNotificationEnabled = settingsPrefs.getBoolean("notifications_enabled", false),
            isAutoPlayVideo = settingsPrefs.getBoolean("auto_play_video", false),
        )
    )

    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        startNetworkMonitoring()
        checkNotificationPermission()
    }

    fun onAction(action: MainAction) {
        when (action) {
            is MainAction.SetDarkMode -> setDarkMode(action.isDark)
            is MainAction.SetContrastMode -> setContrastMode(action.contrastMode)
            is MainAction.SetColorStyle -> setColorStyle(action.colorStyle)
            is MainAction.SetNotificationEnabled -> setNotificationEnabled(action.enabled)
            is MainAction.SetAutoPlayVideo -> setAutoPlayVideo(action.isAutoPlayVideo)
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
            if (isGranted != _state.value.isNotificationEnabled) {
                println("Syncing isNotificationEnabled with permission: newValue=$isGranted")
                setNotificationEnabled(isGranted)
            }
        } else {
            println("POST_NOTIFICATIONS not required (pre-TIRAMISU), setting isNotificationEnabled=true")
            if (!_state.value.isNotificationEnabled) {
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
        _state.update { it.copy(isNotificationEnabled = enabled) }
        settingsPrefs.edit { putBoolean("notifications_enabled", enabled) }
    }

    private fun setAutoPlayVideo(isAutoPlayVideo: Boolean) {
        _state.update { it.copy(isAutoPlayVideo = isAutoPlayVideo) }
        settingsPrefs.edit { putBoolean("auto_play_video", isAutoPlayVideo) }
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
        networkStateMonitor.startMonitoring()
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