package com.example.animeapp.ui.animeHome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeSeasonNowResponse
import com.example.animeapp.models.AnimeSeasonNowSearchQueryState
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.repository.AnimeHomeRepository
import com.example.animeapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val animeSeasonNows: Resource<AnimeSeasonNowResponse> = Resource.Loading(),
    val queryState: AnimeSeasonNowSearchQueryState = AnimeSeasonNowSearchQueryState(),
    val continueWatchingEpisode: EpisodeDetailComplement? = null,
    val isRefreshing: Boolean = false,
    val isShowPopup: Boolean = false,
    val isMinimized: Boolean = false
)

sealed class HomeAction {
    data object GetAnimeSeasonNow : HomeAction()
    data class ApplyFilters(val updatedQueryState: AnimeSeasonNowSearchQueryState) : HomeAction()
    data object FetchContinueWatchingEpisode : HomeAction()
    data class SetMinimized(val minimize: Boolean) : HomeAction()
    data class SetShowPopup(val show: Boolean) : HomeAction()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val animeHomeRepository: AnimeHomeRepository,
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        dispatch(HomeAction.GetAnimeSeasonNow)
    }

    fun dispatch(action: HomeAction) {
        when (action) {
            HomeAction.GetAnimeSeasonNow -> getAnimeSeasonNow()
            is HomeAction.ApplyFilters -> applyFilters(action.updatedQueryState)
            HomeAction.FetchContinueWatchingEpisode -> fetchContinueWatchingEpisode()
            is HomeAction.SetMinimized -> setMinimized(action.minimize)
            is HomeAction.SetShowPopup -> setShowPopup(action.show)
        }
    }

    private fun getAnimeSeasonNow() = viewModelScope.launch {
        _state.update { it.copy(isRefreshing = true, animeSeasonNows = Resource.Loading()) }
        val result = animeHomeRepository.getAnimeSeasonNow(_state.value.queryState)
        _state.update { it.copy(isRefreshing = false, animeSeasonNows = result) }
    }

    private fun applyFilters(updatedQueryState: AnimeSeasonNowSearchQueryState) {
        _state.update { it.copy(queryState = updatedQueryState) }
        dispatch(HomeAction.GetAnimeSeasonNow)
    }

    private fun fetchContinueWatchingEpisode() {
        viewModelScope.launch {
            val episode =
                animeEpisodeDetailRepository.getCachedLatestWatchedEpisodeDetailComplement()
            _state.update { it.copy(continueWatchingEpisode = episode) }
            _state.update { it.copy(isShowPopup = episode != null, isMinimized = false) }
            if (episode != null) {
                delay(10000)
                _state.update { it.copy(isMinimized = true) }
            }
        }
    }

    private fun setMinimized(minimize: Boolean) {
        _state.update { it.copy(isMinimized = minimize) }
    }

    private fun setShowPopup(show: Boolean) {
        _state.update { it.copy(isShowPopup = show) }
    }
}