package com.example.animeapp.ui.animerecommendations

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.ResponseWithPaginationResponse
import com.example.animeapp.repository.AnimeRecommendationsRepository
import com.example.animeapp.utils.Resource
import kotlinx.coroutines.launch
import retrofit2.Response

class AnimeRecommendationsViewModel(
    private val animeRecommendationsRepository: AnimeRecommendationsRepository
) : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "This is recommendation Fragment"
    }
    val text: LiveData<String> = _text

    val animeRecommendations: MutableLiveData<Resource<ResponseWithPaginationResponse>> =
        MutableLiveData()
    var animeRecommendationsPage = 1

    init {
        getAnimeRecommendations()
    }

    fun getAnimeRecommendations() = viewModelScope.launch {
        animeRecommendations.postValue(Resource.Loading())
        val response =
            animeRecommendationsRepository.getAnimeRecommendations(animeRecommendationsPage)
        Log.d("AnimeRecommendationsViewModel", "getAnimeRecommendations: ${response.body()}")
        animeRecommendations.postValue(handleAnimeRecommendationsResponse(response))
    }

    fun handleAnimeRecommendationsResponse(response: Response<ResponseWithPaginationResponse>): Resource<ResponseWithPaginationResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                return Resource.Success(resultResponse)
            }
        }
        return Resource.Error(response.message())
    }
}