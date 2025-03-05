package com.example.animeapp.ui.animeDetail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeAniwatch
import com.example.animeapp.models.AnimeAniwatchSearchResponse
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.models.EpisodesResponse
import com.example.animeapp.repository.AnimeDetailRepository
import com.example.animeapp.repository.AnimeStreamingRepository
import com.example.animeapp.utils.FindAnimeTitle
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import com.example.animeapp.utils.StreamingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
    val animeEpisodeInfo: MutableLiveData<AnimeAniwatch?> = MutableLiveData()
    val defaultEpisodeServers: MutableLiveData<EpisodeServersResponse?> = MutableLiveData()
    val defaultEpisodeSources: MutableLiveData<EpisodeSourcesResponse?> = MutableLiveData()

    fun getAnimeDetail(id: Int) = viewModelScope.launch {
        animeDetail.postValue(Resource.Loading())

        val cachedResponse = getCachedAnimeDetail(id)
        animeDetail.postValue(
            cachedResponse ?: ResponseHandler.handleCommonResponse(
                animeDetailRepository.getAnimeDetail(id)
            )
        )
    }

    private suspend fun getCachedAnimeDetail(id: Int): Resource<AnimeDetailResponse>? {
        val cachedAnimeDetail = animeDetailRepository.getCachedAnimeDetail(id)
        return if (cachedAnimeDetail != null) Resource.Success(cachedAnimeDetail) else null
    }

    fun handleEpisodes() = viewModelScope.launch {
        val detailData = animeDetail.value?.data?.data ?: return@launch episodes.postValue(
            Resource.Error("Anime data not available")
        )
        if (detailData.type == "Music") return@launch episodes.postValue(
            Resource.Error("Anime is a music, no episodes available")
        )
        if (detailData.status == "Not yet aired") return@launch episodes.postValue(
            Resource.Error("Anime not yet aired")
        )
        episodes.postValue(Resource.Loading())
        val title = detailData.title
        val englishTitle = animeDetail.value?.data?.data?.title_english ?: ""
        val searchTitle = when {
            englishTitle.isNotEmpty() -> englishTitle.lowercase()
            else -> title.lowercase()
        }
        val response = animeStreamingRepository.getAnimeAniwatchSearch(searchTitle)
        if (!response.isSuccessful) {
            return@launch episodes.postValue(
                Resource.Error(
                    response.errorBody()?.string() ?: "Unknown error"
                )
            )
        }
        handleValidEpisode(response)
    }

    private fun handleValidEpisode(response: Response<AnimeAniwatchSearchResponse>) =
        viewModelScope.launch {
            if (!response.isSuccessful) {
                episodes.postValue(
                    Resource.Error(
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                )
                return@launch
            }

            val resultResponse = response.body() ?: run {
                episodes.postValue(
                    Resource.Error(
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                )
                return@launch
            }

            val anime =
                FindAnimeTitle.findClosestAnime(resultResponse, animeDetail.value?.data?.data)
                    ?: run {
                        episodes.postValue(Resource.Error("No matching anime found"))
                        return@launch
                    }

            val animeId = anime.id.substringBefore("?").trim()
            val episodesResponse = getEpisodes(animeId)

            if (episodesResponse !is Resource.Success) {
                episodes.postValue(Resource.Error("Failed to fetch episodes"))
                return@launch
            }

            val defaultEpisodeServersResponse = getDefaultEpisodeServers(episodesResponse)

            if (defaultEpisodeServersResponse !is Resource.Success) {
                episodes.postValue(Resource.Error("Failed to fetch episode servers"))
                return@launch
            }

            val defaultEpisodeSourcesResponse =
                StreamingUtils.getEpisodeSources(
                    defaultEpisodeServersResponse,
                    animeStreamingRepository
                )

            if (defaultEpisodeSourcesResponse !is Resource.Success || !checkEpisodeSourceMalId(
                    defaultEpisodeSourcesResponse
                )
            ) {
                episodes.postValue(Resource.Error("No matching anime found"))
                return@launch
            }

            animeEpisodeInfo.postValue(anime)
            episodes.postValue(episodesResponse)
            defaultEpisodeServers.postValue(defaultEpisodeServersResponse.data)
            defaultEpisodeSources.postValue(defaultEpisodeSourcesResponse.data)
        }

    private suspend fun getEpisodes(animeId: String): Resource<EpisodesResponse> =
        viewModelScope.async {
            ResponseHandler.handleCommonResponse(animeStreamingRepository.getEpisodes(animeId))
        }.await()

    private suspend fun getDefaultEpisodeServers(response: Resource<EpisodesResponse>): Resource<EpisodeServersResponse> =
        viewModelScope.async {
            val defaultEpisodeId = response.data?.episodes?.firstOrNull()?.episodeId
                ?: return@async Resource.Error("No default episode found")
            ResponseHandler.handleCommonResponse(
                animeStreamingRepository.getEpisodeServers(
                    defaultEpisodeId
                )
            )
        }.await()

    private fun checkEpisodeSourceMalId(response: Resource<EpisodeSourcesResponse>): Boolean =
        animeDetail.value?.data?.data?.mal_id == response.data?.malID
}