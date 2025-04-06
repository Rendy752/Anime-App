package com.example.animeapp.ui.animeHome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
) : ViewModel() {

    private val _continueWatchingState =
        MutableStateFlow<Resource<EpisodeDetailComplement?>>(Resource.Loading())
    val continueWatchingState: StateFlow<Resource<EpisodeDetailComplement?>> =
        _continueWatchingState.asStateFlow()

    fun fetchContinueWatchingEpisode() {
        viewModelScope.launch {
            try {
                val episode =
                    animeEpisodeDetailRepository.getCachedLatestWatchedEpisodeDetailComplement()
                _continueWatchingState.value = Resource.Success(episode)
            } catch (e: Exception) {
                _continueWatchingState.value = Resource.Error(e.message ?: "An error occurred")
            }
        }
    }
}