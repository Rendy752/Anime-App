package com.luminoverse.animevibe.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class SettingsState(
    val cacheSize: String = "Calculating..."
)

sealed class SettingsAction {
    data object UpdateCacheSize : SettingsAction()
    data object ClearCache : SettingsAction()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val hlsPlayerUtils: HlsPlayerUtils
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        onAction(SettingsAction.UpdateCacheSize)
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.UpdateCacheSize -> updateCacheSize()
            is SettingsAction.ClearCache -> clearCache()
        }
    }

    private fun updateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val sizeInBytes = hlsPlayerUtils.getCacheSize()
            val formattedSize = formatSize(sizeInBytes)
            _state.update { it.copy(cacheSize = formattedSize) }
        }
    }

    private fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            hlsPlayerUtils.release()

            val sizeInBytes = hlsPlayerUtils.getCacheSize()
            val formattedSize = formatSize(sizeInBytes)
            _state.update { it.copy(cacheSize = formattedSize) }
        }
    }


    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val z = (63 - java.lang.Long.numberOfLeadingZeros(bytes)) / 10
        return String.format(Locale.getDefault(), "%.1f %sB", bytes.toDouble() / (1L shl z * 10), " KMGTPE"[z])
    }
}