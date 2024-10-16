package com.example.animeapp.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeRecommendationResponse
import com.example.animeapp.repository.AnimeRecommendationsRepository
import com.example.animeapp.utils.Resource
import kotlinx.coroutines.launch
import retrofit2.Response

class AnimeRecommendationsViewModel(
    private val animeRecommendationsRepository: AnimeRecommendationsRepository
) : ViewModel() {
    val animeRecommendations: MutableLiveData<Resource<AnimeRecommendationResponse>> =
        MutableLiveData()
    var animeRecommendationsPage = 1

    init {
        getAnimeRecommendations()
    }

    fun getAnimeRecommendations() = viewModelScope.launch {
        animeRecommendations.postValue(Resource.Loading())
        val response =
            animeRecommendationsRepository.getAnimeRecommendations(animeRecommendationsPage)
        animeRecommendations.postValue(handleAnimeRecommendationsResponse(response))
    }

    fun handleAnimeRecommendationsResponse(response: Response<AnimeRecommendationResponse>): Resource<AnimeRecommendationResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                return Resource.Success(resultResponse)
            }
        }
        return Resource.Error(response.message())
    }
}