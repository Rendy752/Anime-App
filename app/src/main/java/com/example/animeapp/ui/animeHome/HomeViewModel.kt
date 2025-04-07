package com.example.animeapp.ui.animeHome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.WatchRecentEpisodeResponse
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

    private val _watchRecentEpisode =
        MutableStateFlow<Resource<WatchRecentEpisodeResponse>>(Resource.Loading())
    val watchRecentEpisode: StateFlow<Resource<WatchRecentEpisodeResponse>> =
        _watchRecentEpisode.asStateFlow()

    private val _continueWatchingEpisode =
        MutableStateFlow<Resource<EpisodeDetailComplement?>>(Resource.Loading())
    val continueWatchingEpisode: StateFlow<Resource<EpisodeDetailComplement?>> =
        _continueWatchingEpisode.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        getWatchRecentEpisode()
    }

    fun getWatchRecentEpisode() = viewModelScope.launch {
        _isRefreshing.value = true
        _watchRecentEpisode.value = Resource.Loading()
        _watchRecentEpisode.value =
            animeHomeRepository.getWatchRecentEpisode()
        _isRefreshing.value = false
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