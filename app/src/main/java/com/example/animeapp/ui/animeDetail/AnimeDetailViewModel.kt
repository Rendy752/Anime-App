package com.example.animeapp.ui.animeDetail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeAniwatchSearchResponse
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.models.EpisodesResponse
import com.example.animeapp.repository.AnimeDetailRepository
import com.example.animeapp.repository.AnimeStreamingRepository
import com.example.animeapp.utils.FindAnimeTitle
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
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
        animeDetail.postValue(ResponseHandler.handleCommonResponse(response))
    }

    private suspend fun getCachedAnimeDetail(id: Int): Resource<AnimeDetailResponse>? {
        val cachedAnimeDetail = animeDetailRepository.getCachedAnimeDetail(id)
        return if (cachedAnimeDetail != null) Resource.Success(cachedAnimeDetail) else null
    }

    fun getEpisodes() = viewModelScope.launch {
        episodes.postValue(Resource.Loading())
        val title = animeDetail.value?.data?.data?.title ?: return@launch episodes.postValue(Resource.Error("Title not found"))
        val englishTitle = animeDetail.value?.data?.data?.title_english ?: ""
        val searchTitle = when {
            englishTitle.isNotEmpty() -> englishTitle.lowercase()
            else -> title.lowercase()
        }
        val response = animeStreamingRepository.getAnimeAniwatchSearch(searchTitle)
        handleAnimeSearchResponse(response)
    }

    private fun handleAnimeSearchResponse(
        response: Response<AnimeAniwatchSearchResponse>
    ) {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                val anime =
                    FindAnimeTitle.findClosestAnime(resultResponse, animeDetail.value?.data?.data)

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

    private suspend fun handleEpisodesResponse(response: Response<EpisodesResponse>): Resource<EpisodesResponse> {
        if (!response.isSuccessful) {
            return Resource.Error(response.message() ?: "Failed to fetch episodes")
        }
        val episodesResponse = response.body() ?: return Resource.Error(response.message())
        val episodeDefaultServers = getEpisodeDefaultServers(episodesResponse.episodes[0].episodeId)
        return if (episodeDefaultServers == null) {
            Resource.Error("Failed to fetch episode default servers")
        } else if (fetchEpisodeSources(episodeDefaultServers)) {
            Resource.Success(episodesResponse)
        } else {
            Resource.Error("No episode sources found")
        }
    }

    private suspend fun getEpisodeDefaultServers(episodeId: String): EpisodeSourcesQuery? {
        return try {
            val episodeServersResponse = animeStreamingRepository.getEpisodeServers(episodeId)
            if (episodeServersResponse.isSuccessful) {
                val episodeServers = episodeServersResponse.body()
                episodeServers?.let {
                    if (episodeServers.sub.isNotEmpty()) {
                        EpisodeSourcesQuery(episodeId, episodeServers.sub[0].serverName, "sub")
                    } else if (episodeServers.dub.isNotEmpty()) {
                        EpisodeSourcesQuery(episodeId, episodeServers.dub[0].serverName, "dub")
                    } else if (episodeServers.raw.isNotEmpty()) {
                        EpisodeSourcesQuery(episodeId, episodeServers.raw[0].serverName, "raw")
                    } else {
                        null
                    }
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchEpisodeSources(episodeSourcesQuery: EpisodeSourcesQuery): Boolean =
        try {
            val episodeSourceResponse = animeStreamingRepository.getEpisodeSources(
                episodeSourcesQuery.id,
                episodeSourcesQuery.server,
                episodeSourcesQuery.category
            )
            checkEpisodeSourceMalId(episodeSourceResponse)
        } catch (e: Exception) {
            false
        }

    private fun checkEpisodeSourceMalId(episodeSourceResponse: Response<EpisodeSourcesResponse>): Boolean {
        return if (episodeSourceResponse.isSuccessful) {
            episodeSourceResponse.body()?.let {
                animeDetail.value?.data?.data?.mal_id == it.malID
            } ?: false
        } else {
            false
        }
    }
}