package com.example.animeapp.ui.animeDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeAniwatchSearchResponse
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.utils.AnimeTitleFinder
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.StreamingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository,
) : ViewModel() {

    private val _animeDetail = MutableStateFlow<Resource<AnimeDetailResponse>?>(null)
    val animeDetail: StateFlow<Resource<AnimeDetailResponse>?> = _animeDetail.asStateFlow()

    private val _animeDetailComplement = MutableStateFlow<Resource<AnimeDetailComplement?>?>(null)
    val animeDetailComplement: StateFlow<Resource<AnimeDetailComplement?>?> =
        _animeDetailComplement.asStateFlow()

    private val _defaultEpisode = MutableStateFlow<EpisodeDetailComplement?>(null)
    val defaultEpisode: StateFlow<EpisodeDetailComplement?> = _defaultEpisode.asStateFlow()

    fun handleAnimeDetail(id: Int) = viewModelScope.launch {
        _animeDetail.value = Resource.Loading()
        _animeDetail.value = getAnimeDetail(id)
    }

    suspend fun getAnimeDetail(id: Int): Resource<AnimeDetailResponse> {
        return animeEpisodeDetailRepository.getAnimeDetail(id)
    }

    fun handleEpisodes() = viewModelScope.launch {
        _animeDetailComplement.value = Resource.Loading()
        val detailData = _animeDetail.value?.data?.data
            ?: run {
                _animeDetailComplement.value = Resource.Error("Anime data not available")
                return@launch
            }
        if (handleCachedAnimeDetailComplement(detailData)) return@launch

        if (detailData.type == "Music") {
            _animeDetailComplement.value =
                Resource.Error("Anime is a music, no episodes available")
            return@launch
        }
        val title = detailData.title
        val englishTitle = _animeDetail.value?.data?.data?.title_english ?: ""
        val searchTitle = when {
            englishTitle.isNotEmpty() -> englishTitle.lowercase()
            else -> title.lowercase()
        }
        val response = animeEpisodeDetailRepository.getAnimeAniwatchSearch(searchTitle)
        if (!response.isSuccessful) {
            _animeDetailComplement.value =
                Resource.Error(response.errorBody()?.string() ?: "Unknown error")
            return@launch
        }
        handleValidEpisode(response)
    }

    private suspend fun handleCachedAnimeDetailComplement(detailData: AnimeDetail): Boolean {
        val cachedAnimeDetailComplement =
            animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(detailData.mal_id)

        cachedAnimeDetailComplement?.let { cachedAnimeDetail ->
            val updatedAnimeDetail =
                animeEpisodeDetailRepository.updateAnimeDetailComplementWithEpisodes(
                    detailData,
                    cachedAnimeDetail
                )

            if (updatedAnimeDetail == null) {
                _animeDetailComplement.value = Resource.Error("Failed to fetch or update episodes")
            } else {
                _animeDetailComplement.value = Resource.Success(updatedAnimeDetail)
            }

            cachedAnimeDetail.episodes.firstOrNull()?.let { firstEpisode ->
                val cachedEpisodeDetailComplement =
                    animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(firstEpisode.episodeId)
                if (cachedEpisodeDetailComplement != null) _defaultEpisode.value =
                    cachedEpisodeDetailComplement
                else {
                    val defaultEpisodeServersResponse =
                        getDefaultEpisodeServers(firstEpisode.episodeId)
                    val defaultEpisodeSourcesResponse =
                        StreamingUtils.getEpisodeSources(
                            defaultEpisodeServersResponse,
                            { id, server, category ->
                                animeEpisodeDetailRepository.getEpisodeSources(id, server, category)
                            }
                        )

                    if (defaultEpisodeServersResponse is Resource.Success &&
                        defaultEpisodeSourcesResponse is Resource.Success &&
                        checkEpisodeSourceMalId(defaultEpisodeSourcesResponse)
                    ) {
                        _animeDetail.value?.data?.data?.let { animeDetail ->
                            insertCachedEpisodeDetailComplement(
                                cachedAnimeDetail.id,
                                firstEpisode,
                                defaultEpisodeServersResponse,
                                defaultEpisodeSourcesResponse,
                                animeDetail
                            )
                        }
                    }
                }
            }

            return true
        }
        return false
    }

    private fun handleValidEpisode(response: Response<AnimeAniwatchSearchResponse>) =
        viewModelScope.launch {
            if (!response.isSuccessful) {
                _animeDetailComplement.value =
                    Resource.Error(response.errorBody()?.string() ?: "Unknown error")
                return@launch
            }

            val resultResponse = response.body() ?: run {
                _animeDetailComplement.value =
                    Resource.Error(response.errorBody()?.string() ?: "Unknown error")
                return@launch
            }

            _animeDetail.value?.data?.data?.let { animeDetail ->
                val targetTitles = listOfNotNull(
                    animeDetail.title,
                    animeDetail.title_english
                ) + (animeDetail.title_synonyms ?: emptyList())

                val animes = AnimeTitleFinder.findClosestMatches(
                    targetTitles = targetTitles,
                    data = resultResponse.animes,
                    maxResults = 2,
                    titleExtractor = { it.name }
                )

                if (animes.isEmpty()) {
                    _animeDetailComplement.value = Resource.Error("No episode found")
                    return@launch
                }

                for (anime in animes) {
                    val animeId = anime.id.substringBefore("?").trim()

                    val episodesResponse = animeEpisodeDetailRepository.getEpisodes(animeId)
                    if (episodesResponse !is Resource.Success) {
                        continue
                    }

                    val defaultEpisodeServersResponse =
                        getDefaultEpisodeServers(episodesResponse.data.episodes.firstOrNull()?.episodeId)
                    if (defaultEpisodeServersResponse !is Resource.Success) continue

                    val defaultEpisodeSourcesResponse =
                        StreamingUtils.getEpisodeSources(
                            defaultEpisodeServersResponse,
                            { id, server, category ->
                                animeEpisodeDetailRepository.getEpisodeSources(id, server, category)
                            }
                        )
                    if (defaultEpisodeSourcesResponse !is Resource.Success) continue

                    if (checkEpisodeSourceMalId(defaultEpisodeSourcesResponse)) {
                        val cachedAnimeDetailComplement = AnimeDetailComplement(
                            id = anime.id.substringBefore("?").trim(),
                            malId = animeDetail.mal_id,
                            episodes = episodesResponse.data.episodes,
                            eps = anime.episodes?.eps,
                            sub = anime.episodes?.sub,
                            dub = anime.episodes?.dub,
                        )

                        animeEpisodeDetailRepository.insertCachedAnimeDetailComplement(
                            cachedAnimeDetailComplement
                        )
                        _animeDetailComplement.value =
                            Resource.Success(cachedAnimeDetailComplement)

                        insertCachedEpisodeDetailComplement(
                            cachedAnimeDetailComplement.id,
                            episodesResponse.data.episodes.first(),
                            defaultEpisodeServersResponse,
                            defaultEpisodeSourcesResponse,
                            animeDetail
                        )
                        return@launch
                    }
                }

                _animeDetailComplement.value = Resource.Error("No episode found")
            }
        }

    private fun insertCachedEpisodeDetailComplement(
        aniwatchId: String,
        episode: Episode,
        defaultEpisodeServersResponse: Resource.Success<EpisodeServersResponse>,
        defaultEpisodeSourcesResponse: Resource.Success<EpisodeSourcesResponse>,
        animeDetail: AnimeDetail
    ) = viewModelScope.launch {
        defaultEpisodeServersResponse.data.let { servers ->
            defaultEpisodeSourcesResponse.data.let { sources ->
                val cachedEpisodeDetailComplement =
                    StreamingUtils.getEpisodeQuery(
                        Resource.Success(servers),
                        servers.episodeId
                    )?.let { query ->
                        EpisodeDetailComplement(
                            id = episode.episodeId,
                            malId = animeDetail.mal_id,
                            aniwatchId = aniwatchId,
                            animeTitle = animeDetail.title,
                            episodeTitle = episode.name,
                            imageUrl = animeDetail.images.jpg.image_url,
                            number = episode.episodeNo,
                            isFiller = episode.filler,
                            servers = servers,
                            sources = sources,
                            sourcesQuery = query
                        )
                    }
                cachedEpisodeDetailComplement?.let {
                    animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(it)
                    _defaultEpisode.value = it
                }
            }
        }
    }

    suspend fun getCachedEpisodeDetailComplement(episodeId: String): EpisodeDetailComplement? =
        animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId)

    private suspend fun getDefaultEpisodeServers(defaultEpisodeId: String?): Resource<EpisodeServersResponse> =
        viewModelScope.async {
            defaultEpisodeId ?: return@async Resource.Error("No default episode found")
            animeEpisodeDetailRepository.getEpisodeServers(defaultEpisodeId)
        }.await()

    private fun checkEpisodeSourceMalId(response: Resource<EpisodeSourcesResponse>): Boolean =
        _animeDetail.value?.data?.data?.mal_id == response.data?.malID

    fun handleToggleFavorite(favorite: Boolean) {
        viewModelScope.launch {
            if (_animeDetailComplement.value !is Resource.Success) return@launch
            _animeDetailComplement.value?.data?.let {
                val updatedComplement = it.copy(isFavorite = favorite)
                _animeDetailComplement.value = Resource.Success(updatedComplement)
                animeEpisodeDetailRepository.updateAnimeDetailComplement(updatedComplement)
            }
        }
    }
}