package com.example.animeapp.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.repository.AnimeDetailRepository
import com.example.animeapp.utils.Resource
import kotlinx.coroutines.launch
import retrofit2.Response

class AnimeDetailViewModel(
    private val animeDetailRepository: AnimeDetailRepository
) : ViewModel() {
    val animeDetail: MutableLiveData<Resource<AnimeDetailResponse>> = MutableLiveData()

    fun getAnimeDetail(id: Int) = viewModelScope.launch {
        animeDetail.postValue(Resource.Loading())
        val response = animeDetailRepository.getAnimeDetail(id)
        animeDetail.postValue(handleAnimeDetailResponse(response))
    }

    fun handleAnimeDetailResponse(response: Response<AnimeDetailResponse>): Resource<AnimeDetailResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                return Resource.Success(resultResponse)
            }
        }
        return Resource.Error(response.message())
    }
}