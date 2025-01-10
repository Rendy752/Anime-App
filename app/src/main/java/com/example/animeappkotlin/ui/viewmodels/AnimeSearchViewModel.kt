package com.example.animeappkotlin.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeappkotlin.models.AnimeSearchResponse
import com.example.animeappkotlin.repository.AnimeSearchRepository
import com.example.animeappkotlin.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class AnimeSearchViewModel(
    private val animeSearchRepository: AnimeSearchRepository
) : ViewModel() {

    private val _animeSearchResults = MutableStateFlow<Resource<AnimeSearchResponse>>(Resource.Loading())
    val animeSearchResults: StateFlow<Resource<AnimeSearchResponse>> = _animeSearchResults.asStateFlow()

    fun searchAnime(query: String) = viewModelScope.launch {
        _animeSearchResults.value = Resource.Loading()
        val response = animeSearchRepository.searchAnime(query)
        _animeSearchResults.value = handleAnimeSearchResponse(response)
    }

    private fun handleAnimeSearchResponse(response: Response<AnimeSearchResponse>): Resource<AnimeSearchResponse> {
        return if (response.isSuccessful) {
            Log.d("AnimeSearchViewModel", "Response successful: ${response.body()}")
            response.body()?.let { resultResponse ->
                Resource.Success(resultResponse)
            } ?: Resource.Error("Response body is null")
        } else {
            Log.e("AnimeSearchViewModel", "Response error: ${response.message()}")
            Resource.Error(response.message())
        }
    }
}