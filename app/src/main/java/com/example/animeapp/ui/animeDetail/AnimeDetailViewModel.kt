package com.example.animeapp.ui.animeDetail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeAniwatchSearchResponse
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.models.EpisodesResponse
import com.example.animeapp.repository.AnimeDetailRepository
import com.example.animeapp.repository.AnimeStreamingRepository
import com.example.animeapp.utils.CompareUtils
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
    val animeDetailComplement: MutableLiveData<Resource<AnimeDetailComplement?>> = MutableLiveData()
    val defaultEpisode: MutableLiveData<EpisodeDetailComplement?> = MutableLiveData()

    fun handleAnimeDetail(id: Int) = viewModelScope.launch {
        animeDetail.postValue(Resource.Loading())
        animeDetail.postValue(getAnimeDetail(id))
    }

    suspend fun getAnimeDetail(id: Int): Resource<AnimeDetailResponse> {
        return ResponseHandler.handleCommonResponse(animeDetailRepository.getAnimeDetail(id))
    }

    fun handleEpisodes() = viewModelScope.launch {
        animeDetailComplement.postValue(Resource.Loading())
        val detailData =
            animeDetail.value?.data?.data
                ?: return@launch animeDetailComplement.postValue(Resource.Error("Anime data not available"))
        if (handleCachedAnimeDetail(detailData)) return@launch

        if (detailData.type == "Music") return@launch animeDetailComplement.postValue(
            Resource.Error("Anime is a music, no episodes available")
        )
        if (detailData.status == "Not yet aired") return@launch animeDetailComplement.postValue(
            Resource.Error("Anime not yet aired")
        )
        val title = detailData.title
        val englishTitle = animeDetail.value?.data?.data?.title_english ?: ""
        val searchTitle = when {
            englishTitle.isNotEmpty() -> englishTitle.lowercase()
            else -> title.lowercase()
        }
        val response = animeStreamingRepository.getAnimeAniwatchSearch(searchTitle)
        if (!response.isSuccessful) {
            return@launch animeDetailComplement.postValue(
                Resource.Error(
                    response.errorBody()?.string() ?: "Unknown error"
                )
            )
        }
        handleValidEpisode(response)
    }

    private suspend fun handleCachedAnimeDetail(detailData: AnimeDetail): Boolean {
        val cachedAnimeDetailComplement =
            animeDetailRepository.getCachedAnimeDetailComplementByMalId(detailData.mal_id)
        cachedAnimeDetailComplement?.let { cachedAnimeDetail ->
            val defaultEpisodeId = cachedAnimeDetail.episodes.firstOrNull()?.episodeId
            if (detailData.airing) {
                val episodesResponse = getEpisodes(cachedAnimeDetail.id)
                if (episodesResponse !is Resource.Success) {
                    animeDetailComplement.postValue(Resource.Error("Failed to fetch episodes"))
                } else {
                    if (CompareUtils.areDataClassesEqual(
                            episodesResponse.data?.episodes,
                            cachedAnimeDetail.episodes
                        )
                    ) {
                        animeDetailComplement.postValue(Resource.Success(cachedAnimeDetail))
                    } else {
                        val updatedCachedAnimeDetail =
                            episodesResponse.data?.let { cachedAnimeDetail.copy(episodes = it.episodes) }
                        updatedCachedAnimeDetail?.let {
                            animeDetailRepository.updateCachedAnimeDetailComplement(
                                it
                            )
                        }
                        animeDetailComplement.postValue(Resource.Success(updatedCachedAnimeDetail))
                    }
                }
            } else {
                animeDetailComplement.postValue(Resource.Success(cachedAnimeDetail))
            }
            defaultEpisodeId?.let {
                val cachedEpisodeDetailComplement =
                    animeDetailRepository.getCachedEpisodeDetailComplement(it)
                defaultEpisode.postValue(cachedEpisodeDetailComplement)
            }
            return true
        }
        return false
    }

    private fun handleValidEpisode(response: Response<AnimeAniwatchSearchResponse>) =
        viewModelScope.launch {
            if (!response.isSuccessful) {
                animeDetailComplement.postValue(
                    Resource.Error(
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                )
                return@launch
            }

            val resultResponse = response.body() ?: run {
                animeDetailComplement.postValue(
                    Resource.Error(
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                )
                return@launch
            }

            val anime =
                FindAnimeTitle.findClosestAnime(resultResponse, animeDetail.value?.data?.data)
                    ?: run {
                        animeDetailComplement.postValue(Resource.Error("No matching anime found"))
                        return@launch
                    }

            val animeId = anime.id.substringBefore("?").trim()
            val episodesResponse = getEpisodes(animeId)

            if (episodesResponse !is Resource.Success) {
                animeDetailComplement.postValue(Resource.Error("Failed to fetch episodes"))
                return@launch
            }

            val defaultEpisodeServersResponse =
                getDefaultEpisodeServers(episodesResponse.data?.episodes?.firstOrNull()?.episodeId)

            if (defaultEpisodeServersResponse !is Resource.Success) {
                animeDetailComplement.postValue(Resource.Error("Failed to fetch episode servers"))
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
                animeDetailComplement.postValue(Resource.Error("No matching anime found"))
                return@launch
            }

            val cachedAnimeDetailComplement = animeDetail.value?.data?.data?.mal_id?.let {
                AnimeDetailComplement(
                    id = anime.id,
                    mal_id = it,
                    episodes = episodesResponse.data?.episodes ?: emptyList(),
                    eps = anime.episodes?.eps,
                    sub = anime.episodes?.sub,
                    dub = anime.episodes?.dub,
                )
            }
            cachedAnimeDetailComplement?.let {
                animeDetailRepository.insertCachedAnimeDetailComplement(it)
            }
            animeDetailComplement.postValue(
                Resource.Success(cachedAnimeDetailComplement)
            )

            defaultEpisodeServersResponse.data?.let { servers ->
                defaultEpisodeSourcesResponse.data?.let { sources ->
                    val cachedEpisodeDetailComplement = EpisodeDetailComplement(
                        id = servers.episodeId,
                        servers = servers,
                        sources = sources
                    )
                    animeDetailRepository.insertCachedEpisodeDetailComplement(
                        cachedEpisodeDetailComplement
                    )
                    defaultEpisode.postValue(cachedEpisodeDetailComplement)
                }
            }
        }

    private suspend fun getEpisodes(animeId: String): Resource<EpisodesResponse> =
        viewModelScope.async {
            ResponseHandler.handleCommonResponse(animeStreamingRepository.getEpisodes(animeId))
        }.await()

    private suspend fun getDefaultEpisodeServers(defaultEpisodeId: String?): Resource<EpisodeServersResponse> =
        viewModelScope.async {
            defaultEpisodeId ?: return@async Resource.Error("No default episode found")
            ResponseHandler.handleCommonResponse(
                animeStreamingRepository.getEpisodeServers(
                    defaultEpisodeId
                )
            )
        }.await()

    private fun checkEpisodeSourceMalId(response: Resource<EpisodeSourcesResponse>): Boolean =
        animeDetail.value?.data?.data?.mal_id == response.data?.malID
}