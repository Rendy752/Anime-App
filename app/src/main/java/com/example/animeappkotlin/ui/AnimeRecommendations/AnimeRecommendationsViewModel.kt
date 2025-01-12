package com.example.animeappkotlin.ui.AnimeRecommendations

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeappkotlin.models.AnimeRecommendationResponse
import com.example.animeappkotlin.repository.AnimeRecommendationsRepository
import com.example.animeappkotlin.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class AnimeRecommendationsViewModel(
    private val animeRecommendationsRepository: AnimeRecommendationsRepository
) : ViewModel() {

    private val _animeRecommendations = MutableStateFlow<Resource<AnimeRecommendationResponse>>(Resource.Loading())
    val animeRecommendations: StateFlow<Resource<AnimeRecommendationResponse>> = _animeRecommendations.asStateFlow()

    private var animeRecommendationsPage = 1

    init {
        getAnimeRecommendations()
    }

    private fun getAnimeRecommendations() = viewModelScope.launch {
        _animeRecommendations.value = Resource.Loading()
        val response = animeRecommendationsRepository.getAnimeRecommendations(animeRecommendationsPage)
        _animeRecommendations.value = handleAnimeRecommendationsResponse(response)
    }

    private fun handleAnimeRecommendationsResponse(response: Response<AnimeRecommendationResponse>): Resource<AnimeRecommendationResponse> {
        return if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                Resource.Success(resultResponse)
            } ?: Resource.Error("Response body is null")
        } else {
            Resource.Error(response.message())
        }
    }

    fun refreshData() {
        getAnimeRecommendations()
    }
}