package com.example.animeapp.ui.animeDetail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeAniwatchSearchResponse
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.EpisodesResponse
import com.example.animeapp.repository.AnimeDetailRepository
import com.example.animeapp.repository.AnimeStreamingRepository
import com.example.animeapp.utils.FindAnimeTitle
import com.example.animeapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    private val animeDetailRepository: AnimeDetailRepository,
    private val animeStreamingRepository: AnimeStreamingRepository
) : ViewModel() {
    val animeDetail: MutableLiveData<Resource<AnimeDetailResponse>?> = MutableLiveData()
    val episodes: MutableLiveData<Resource<EpisodesResponse>?> = MutableLiveData()

    fun getAnimeDetail(id: Int) = viewModelScope.launch {
        val cachedResponse = getCachedAnimeDetail(id)
        if (cachedResponse != null) {
            animeDetail.postValue(cachedResponse)
            return@launch
        }

        animeDetail.postValue(Resource.Loading())
        val response = animeDetailRepository.getAnimeDetail(id)
        animeDetail.postValue(handleAnimeDetailResponse(response))
    }

    fun getEpisodes() = viewModelScope.launch {
        episodes.postValue(Resource.Loading())
        val title = animeDetail.value?.data?.data?.title ?: ""
        val englishTitle = animeDetail.value?.data!!.data.title_english ?: ""
        val searchTitle =
            if (englishTitle.isNotEmpty()) englishTitle.lowercase() else title.lowercase()

        val response = searchTitle.let { animeStreamingRepository.getAnimeAniwatchSearch(it) }
        handleAnimeSearchResponse(response)
    }

    private fun handleAnimeSearchResponse(
        response: Response<AnimeAniwatchSearchResponse>
    ) {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                val anime = FindAnimeTitle.findClosestAnime(resultResponse, animeDetail.value?.data?.data)

                if (anime != null) {
                    val animeId = anime.id.substringBefore("?").trim()
                    fetchEpisodesForAnime(animeId)
                } else {
                    episodes.postValue(Resource.Error("No matching anime found"))
                }
            } ?: run {
                episodes.postValue(Resource.Error(response.message()))
            }
        } else {
            episodes.postValue(Resource.Error(response.message()))
        }
    }

    private fun fetchEpisodesForAnime(animeId: String) = viewModelScope.launch {
        try {
            val episodesResponse = animeStreamingRepository.getEpisodes(animeId)
            episodes.postValue(handleEpisodesResponse(episodesResponse))
        } catch (e: Exception) {
            episodes.postValue(Resource.Error(e.message ?: "Failed to fetch episodes"))
        }
    }

    private fun handleEpisodesResponse(response: Response<EpisodesResponse>): Resource<EpisodesResponse> {
        return if (response.isSuccessful) {
            response.body()?.let {
                Resource.Success(it)
            } ?: Resource.Error(response.message())
        } else {
            Resource.Error(response.message())
        }
    }

    private suspend fun handleAnimeDetailResponse(response: Response<AnimeDetailResponse>): Resource<AnimeDetailResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                cacheAnimeDetail(resultResponse)
                return Resource.Success(resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    private suspend fun getCachedAnimeDetail(id: Int): Resource<AnimeDetailResponse>? {
        val cachedAnimeDetail = animeDetailRepository.getCachedAnimeDetail(id)
        return if (cachedAnimeDetail != null) {
            Resource.Success(cachedAnimeDetail)
        } else {
            null
        }
    }

    private suspend fun cacheAnimeDetail(animeDetailResponse: AnimeDetailResponse) {
        animeDetailRepository.cacheAnimeDetail(animeDetailResponse)
    }
}