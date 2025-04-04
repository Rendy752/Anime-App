package com.example.animeapp.ui.animeRecommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeRecommendationResponse
import com.example.animeapp.repository.AnimeRecommendationsRepository
import com.example.animeapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimeRecommendationsViewModel @Inject constructor(
    private val animeRecommendationsRepository: AnimeRecommendationsRepository
) : ViewModel() {
    private val _animeRecommendations =
        MutableStateFlow<Resource<AnimeRecommendationResponse>>(Resource.Loading())
    val animeRecommendations: StateFlow<Resource<AnimeRecommendationResponse>> =
        _animeRecommendations.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var animeRecommendationsPage = 1

    init {
        getAnimeRecommendations()
    }

    fun getAnimeRecommendations() = viewModelScope.launch {
        _isRefreshing.value = true
        _animeRecommendations.value = Resource.Loading()
        _animeRecommendations.value =
            animeRecommendationsRepository.getAnimeRecommendations(animeRecommendationsPage)
        _isRefreshing.value = false
    }
}