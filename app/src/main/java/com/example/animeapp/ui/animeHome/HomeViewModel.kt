package com.example.animeapp.ui.animeHome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.AnimeSeasonNowResponse
import com.example.animeapp.models.AnimeSeasonNowSearchQueryState
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.repository.AnimeHomeRepository
import com.example.animeapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val animeHomeRepository: AnimeHomeRepository,
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
) : ViewModel() {

    private val _animeSeasonNows =
        MutableStateFlow<Resource<AnimeSeasonNowResponse>>(Resource.Loading())
    val animeSeasonNows: StateFlow<Resource<AnimeSeasonNowResponse>> =
        _animeSeasonNows.asStateFlow()

    private val _queryState = MutableStateFlow(AnimeSeasonNowSearchQueryState())
    val queryState: StateFlow<AnimeSeasonNowSearchQueryState> = _queryState.asStateFlow()

    private val _continueWatchingEpisode =
        MutableStateFlow<Resource<EpisodeDetailComplement?>>(Resource.Loading())
    val continueWatchingEpisode: StateFlow<Resource<EpisodeDetailComplement?>> =
        _continueWatchingEpisode.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        getAnimeSeasonNow()
    }

    fun getAnimeSeasonNow() = viewModelScope.launch {
        _isRefreshing.value = true
        _animeSeasonNows.value = Resource.Loading()
        _animeSeasonNows.value = animeHomeRepository.getAnimeSeasonNow(_queryState.value)
        _isRefreshing.value = false
    }

    fun applyFilters(updatedQueryState: AnimeSeasonNowSearchQueryState) {
        _queryState.value = updatedQueryState
        viewModelScope.launch {
            getAnimeSeasonNow()
        }
    }

    fun fetchContinueWatchingEpisode() {
        viewModelScope.launch {
            try {
                val episode =
                    animeEpisodeDetailRepository.getCachedLatestWatchedEpisodeDetailComplement()
                _continueWatchingEpisode.value = Resource.Success(episode)
            } catch (e: Exception) {
                _continueWatchingEpisode.value = Resource.Error(e.message ?: "An error occurred")
            }
        }
    }
}