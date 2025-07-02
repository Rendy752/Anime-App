package com.luminoverse.animevibe.ui.main

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminoverse.animevibe.models.NetworkStatus
import com.luminoverse.animevibe.models.networkStatusPlaceholder
import com.luminoverse.animevibe.ui.common.SharedImageState
import com.luminoverse.animevibe.ui.theme.ColorStyle
import com.luminoverse.animevibe.ui.theme.ContrastMode
import com.luminoverse.animevibe.ui.theme.ThemeMode
import com.luminoverse.animevibe.utils.NetworkStateMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SnackbarMessageType {
    INFO, SUCCESS, ERROR
}

/**
 * Data class to hold all information for displaying a snackbar.
 *
 * @param message The text to display in the snackbar.
 * @param type The type of message, which can affect styling or icons.
 * @param actionLabel Optional text for an action button on the snackbar.
 * @param onAction Optional lambda to execute when the action button is pressed.
 * @param id A unique identifier to ensure the snackbar is re-triggered even with the same message.
 */
data class SnackbarMessage(
    val message: String,
    val type: SnackbarMessageType = SnackbarMessageType.INFO,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val id: Long = System.currentTimeMillis()
)

enum class PlayerDisplayMode {
    FULLSCREEN,
    PIP
}

data class PlayerState(
    val malId: Int,
    val episodeId: String,
    val displayMode: PlayerDisplayMode = PlayerDisplayMode.FULLSCREEN,
    val pipOffset: IntOffset = IntOffset.Zero
)

data class MainState(
    val themeMode: ThemeMode = ThemeMode.System,
    val isDarkMode: Boolean = false,
    val contrastMode: ContrastMode = ContrastMode.Normal,
    val colorStyle: ColorStyle = ColorStyle.Default,
    val isNotificationEnabled: Boolean = false,
    val isAutoPlayVideo: Boolean = true,
    val isRtl: Boolean = false,
    val networkStatus: NetworkStatus = networkStatusPlaceholder,
    val isShowIdleDialog: Boolean = false,
    val isLandscape: Boolean = false,
    val sharedImageState: SharedImageState? = null,
    val snackbarMessage: SnackbarMessage? = null,
    val playerState: PlayerState? = null
)

sealed class MainAction {
    data class SetThemeMode(val themeMode: ThemeMode) : MainAction()
    data class SetContrastMode(val contrastMode: ContrastMode) : MainAction()
    data class SetColorStyle(val colorStyle: ColorStyle) : MainAction()
    data class SetNotificationEnabled(val enabled: Boolean) : MainAction()
    data class SetAutoPlayVideo(val isAutoPlayVideo: Boolean) : MainAction()
    data class SetRtl(val isRtl: Boolean) : MainAction()
    data class SetNetworkStatus(val status: NetworkStatus) : MainAction()
    data class SetIsShowIdleDialog(val show: Boolean) : MainAction()
    data object CheckNotificationPermission : MainAction()
    data object SyncSystemDarkMode : MainAction()
    data class ShowImagePreview(val state: SharedImageState) : MainAction()
    data object DismissImagePreview : MainAction()
    data class ShowSnackbar(val message: SnackbarMessage) : MainAction()
    data object DismissSnackbar : MainAction()
    data class PlayEpisode(val malId: Int, val episodeId: String) : MainAction()
    data class UpdatePlayerPipOffset(val offset: IntOffset) : MainAction()
    data class SetPlayerDisplayMode(val mode: PlayerDisplayMode) : MainAction()
    object ClosePlayer : MainAction()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val app: Application,
    private val networkStateMonitor: NetworkStateMonitor
) : AndroidViewModel(app) {

    private val themePrefs = app.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    private val settingsPrefs =
        app.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _state: MutableStateFlow<MainState>

    val state: StateFlow<MainState>

    private val configurationChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                onAction(MainAction.SyncSystemDarkMode)
            }
        }
    }

    init {
        val initialThemeMode = themePrefs.getString("theme_mode", ThemeMode.System.name)
            ?.let { ThemeMode.valueOf(it) } ?: ThemeMode.System

        _state = MutableStateFlow(
            MainState(
                themeMode = initialThemeMode,
                isDarkMode = isDarkMode(initialThemeMode),
                contrastMode = themePrefs.getString("contrast_mode", ContrastMode.Normal.name)
                    ?.let { ContrastMode.valueOf(it) } ?: ContrastMode.Normal,
                colorStyle = themePrefs.getString("color_style", ColorStyle.Default.name)
                    ?.let { ColorStyle.valueOf(it) } ?: ColorStyle.Default,
                isNotificationEnabled = settingsPrefs.getBoolean("notifications_enabled", false),
                isAutoPlayVideo = settingsPrefs.getBoolean("auto_play_video", true),
                isRtl = settingsPrefs.getBoolean("rtl", false)
            )
        )
        state = _state.asStateFlow()

        app.registerReceiver(
            configurationChangeReceiver,
            IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)
        )

        startNetworkMonitoring()
        checkNotificationPermission()
    }

    fun onAction(action: MainAction) {
        when (action) {
            is MainAction.SetThemeMode -> setThemeMode(action.themeMode)
            is MainAction.SyncSystemDarkMode -> syncSystemDarkMode()
            is MainAction.SetContrastMode -> setContrastMode(action.contrastMode)
            is MainAction.SetColorStyle -> setColorStyle(action.colorStyle)
            is MainAction.SetNotificationEnabled -> setNotificationEnabled(action.enabled)
            is MainAction.SetAutoPlayVideo -> setAutoPlayVideo(action.isAutoPlayVideo)
            is MainAction.SetRtl -> setRtl(action.isRtl)
            is MainAction.SetNetworkStatus -> setNetworkStatus(action.status)
            is MainAction.SetIsShowIdleDialog -> setIsShowIdleDialog(action.show)
            is MainAction.CheckNotificationPermission -> checkNotificationPermission()
            is MainAction.ShowImagePreview -> _state.update { it.copy(sharedImageState = action.state) }
            is MainAction.DismissImagePreview -> _state.update { it.copy(sharedImageState = null) }
            is MainAction.ShowSnackbar -> showSnackbar(action.message)
            is MainAction.DismissSnackbar -> dismissSnackbar()
            is MainAction.PlayEpisode -> playEpisode(action.malId, action.episodeId)
            is MainAction.UpdatePlayerPipOffset -> updatePlayerPipOffset(action.offset)
            is MainAction.SetPlayerDisplayMode -> setPlayerDisplayMode(action.mode)
            is MainAction.ClosePlayer -> closePlayer()
        }
    }

    private fun showSnackbar(message: SnackbarMessage) {
        _state.update { it.copy(snackbarMessage = message) }
    }

    private fun dismissSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    private fun playEpisode(malId: Int, episodeId: String) {
        _state.update {
            it.copy(
                playerState = PlayerState(
                    malId = malId,
                    episodeId = episodeId,
                    displayMode = PlayerDisplayMode.FULLSCREEN
                )
            )
        }
    }

    private fun updatePlayerPipOffset(offset: IntOffset) {
        _state.value.playerState?.let { current ->
            _state.update {
                it.copy(playerState = current.copy(pipOffset = offset))
            }
        }
    }

    private fun setPlayerDisplayMode(mode: PlayerDisplayMode) {
        _state.value.playerState?.let { current ->
            _state.update {
                it.copy(playerState = current.copy(displayMode = mode))
            }
        }
    }

    private fun closePlayer() {
        _state.update { it.copy(playerState = null) }
    }

    private fun isDarkMode(themeMode: ThemeMode): Boolean {
        return when (themeMode) {
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
            ThemeMode.System -> {
                val uiMode = app.resources.configuration.uiMode
                (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun syncSystemDarkMode() {
        if (_state.value.themeMode == ThemeMode.System) {
            _state.update { currentState ->
                val newIsDarkMode = isDarkMode(ThemeMode.System)
                if (currentState.isDarkMode != newIsDarkMode) {
                    currentState.copy(isDarkMode = newIsDarkMode)
                } else {
                    currentState
                }
            }
        }
    }

    private fun setThemeMode(newThemeMode: ThemeMode) {
        _state.update {
            it.copy(
                themeMode = newThemeMode,
                isDarkMode = isDarkMode(newThemeMode)
            )
        }
        themePrefs.edit { putString("theme_mode", newThemeMode.name) }
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

    private fun setRtl(isRtl: Boolean) {
        _state.update { it.copy(isRtl = isRtl) }
        settingsPrefs.edit { putBoolean("rtl", isRtl) }
    }

    private fun setNetworkStatus(status: NetworkStatus) {
        _state.update { it.copy(networkStatus = status) }
    }

    private fun setIsShowIdleDialog(show: Boolean) {
        _state.update { it.copy(isShowIdleDialog = show) }
    }

    fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                getApplication(),
                "android.permission.POST_NOTIFICATIONS"
            )
            val isGranted = permissionStatus == PackageManager.PERMISSION_GRANTED
            if (isGranted != _state.value.isNotificationEnabled) {
                setNotificationEnabled(isGranted)
            }
        } else {
            if (!_state.value.isNotificationEnabled) {
                setNotificationEnabled(true)
            }
        }
    }

    private fun startNetworkMonitoring() {
        networkStateMonitor.startMonitoring()
        viewModelScope.launch {
            networkStateMonitor.networkStatus.collect { status ->
                onAction(MainAction.SetNetworkStatus(status))
            }
        }
    }

    public override fun onCleared() {
        super.onCleared()
        app.unregisterReceiver(configurationChangeReceiver)
        networkStateMonitor.stopMonitoring()
    }
}