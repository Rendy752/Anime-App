package com.luminoverse.animevibe.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.animeDetailPlaceholder
import com.luminoverse.animevibe.repository.AnimeSearchRepository
import com.luminoverse.animevibe.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val animeDetailSample: Resource<AnimeDetail> = Resource.Loading(),
)

sealed class SettingsAction {
    data object GetRandomAnime : SettingsAction()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val animeSearchRepository: AnimeSearchRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        onAction(SettingsAction.GetRandomAnime)
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.GetRandomAnime -> getRandomAnime()
        }
    }

    private fun getRandomAnime() = viewModelScope.launch {
        _state.update { it.copy(animeDetailSample = Resource.Loading()) }
        val animeSearchResponseResource = animeSearchRepository.getRandomAnime()
        _state.update {
            it.copy(
                animeDetailSample = if (animeSearchResponseResource is Resource.Success) {
                    Resource.Success(animeSearchResponseResource.data.data.first())
                } else {
                    Resource.Success(animeDetailPlaceholder)
                }
            )
        }
    }
}