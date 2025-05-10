package com.example.animeapp.ui.animeDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.*
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.utils.AnimeTitleFinder
import com.example.animeapp.utils.AnimeTitleFinder.normalizeTitle
import com.example.animeapp.utils.FilterUtils
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.StreamingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

data class DetailState(
    val animeDetail: Resource<AnimeDetailResponse> = Resource.Loading(),
    val animeDetailComplement: Resource<AnimeDetailComplement?> = Resource.Loading(),
    val defaultEpisodeId: String? = null,
    val relationAnimeDetails: Map<Int, Resource<AnimeDetail>> = emptyMap(),
    val episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>> = emptyMap()
)

data class EpisodeFilterState(
    val episodeQuery: FilterUtils.EpisodeQueryState = FilterUtils.EpisodeQueryState(),
    val filteredEpisodes: List<Episode> = emptyList()
)

sealed class DetailAction {
    data class LoadAnimeDetail(val id: Int) : DetailAction()
    data class LoadRelationAnimeDetail(val id: Int) : DetailAction()
    data class LoadEpisodeDetailComplement(val episodeId: String) : DetailAction()
    data class LoadEpisodes(val isRefresh: Boolean = false) : DetailAction()
    data class UpdateEpisodeQueryState(val query: FilterUtils.EpisodeQueryState) : DetailAction()
    data class ToggleFavorite(val isFavorite: Boolean) : DetailAction()
}

@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository,
) : ViewModel() {

    private val _detailState = MutableStateFlow(DetailState())
    val detailState: StateFlow<DetailState> = _detailState.asStateFlow()

    private val _episodeFilterState = MutableStateFlow(EpisodeFilterState())
    val episodeFilterState: StateFlow<EpisodeFilterState> = _episodeFilterState.asStateFlow()

    private var isLoadingEpisodes = false
    private var isLoadingEpisodeDetail = false

    fun onAction(action: DetailAction) {
        when (action) {
            is DetailAction.LoadAnimeDetail -> loadAnimeDetail(action.id)
            is DetailAction.LoadRelationAnimeDetail -> loadRelationAnimeDetail(action.id)
            is DetailAction.LoadEpisodeDetailComplement -> loadEpisodeDetailComplement(action.episodeId)
            is DetailAction.LoadEpisodes -> loadEpisodes(action.isRefresh)
            is DetailAction.UpdateEpisodeQueryState -> updateEpisodeQueryState(action.query)
            is DetailAction.ToggleFavorite -> handleToggleFavorite(action.isFavorite)
        }
    }

    private fun loadAnimeDetail(id: Int) = viewModelScope.launch {
        _detailState.update { it.copy(animeDetail = Resource.Loading()) }
        val result = animeEpisodeDetailRepository.getAnimeDetail(id)
        _detailState.update { it.copy(animeDetail = result) }
        if (result is Resource.Success) {
            loadEpisodes()
        }
    }

    private fun loadRelationAnimeDetail(id: Int) = viewModelScope.launch {
        _detailState.update {
            it.copy(relationAnimeDetails = it.relationAnimeDetails + (id to Resource.Loading()))
        }
        val result = animeEpisodeDetailRepository.getAnimeDetail(id)
        val animeDetail = when (result) {
            is Resource.Success -> Resource.Success(result.data.data)
            is Resource.Error -> Resource.Error(result.message ?: "Error loading relation")
            is Resource.Loading -> Resource.Loading()
        }
        _detailState.update {
            it.copy(relationAnimeDetails = it.relationAnimeDetails + (id to animeDetail))
        }
    }

    private fun loadEpisodeDetailComplement(episodeId: String) = viewModelScope.launch {
        if (isLoadingEpisodeDetail) {
            return@launch
        }
        isLoadingEpisodeDetail = true
        try {
            _detailState.update {
                it.copy(episodeDetailComplements = it.episodeDetailComplements + (episodeId to Resource.Loading()))
            }
            val cachedComplement =
                animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId)
            if (cachedComplement != null) {
                _detailState.update {
                    it.copy(
                        episodeDetailComplements = it.episodeDetailComplements + (episodeId to Resource.Success(
                            cachedComplement
                        ))
                    )
                }
                return@launch
            }
            val animeDetail = _detailState.value.animeDetail.data?.data ?: run {
                _detailState.update {
                    it.copy(
                        episodeDetailComplements = it.episodeDetailComplements + (episodeId to Resource.Error(
                            "Anime data not available"
                        ))
                    )
                }
                return@launch
            }
            val animeDetailComplement = _detailState.value.animeDetailComplement.data ?: run {
                _detailState.update {
                    it.copy(
                        episodeDetailComplements = it.episodeDetailComplements + (episodeId to Resource.Error(
                            "Anime complement not available"
                        ))
                    )
                }
                return@launch
            }
            val episode =
                animeDetailComplement.episodes?.find { it.episodeId == episodeId } ?: run {
                    _detailState.update {
                        it.copy(
                            episodeDetailComplements = it.episodeDetailComplements + (episodeId to Resource.Error(
                                "Episode not found"
                            ))
                        )
                    }
                    return@launch
                }
            val serversResponse = getDefaultEpisodeServers(episodeId)
            val sourcesResponse = StreamingUtils.getEpisodeSources(
                serversResponse,
                { id, server, category ->
                    animeEpisodeDetailRepository.getEpisodeSources(
                        id,
                        server,
                        category
                    )
                }
            )
            if (serversResponse is Resource.Success && sourcesResponse is Resource.Success && checkEpisodeSourceMalId(
                    sourcesResponse
                )
            ) {
                val complement = StreamingUtils.getEpisodeQuery(
                    Resource.Success(serversResponse.data),
                    episodeId
                )?.let { query ->
                    EpisodeDetailComplement(
                        id = episodeId,
                        malId = animeDetail.mal_id,
                        aniwatchId = animeDetailComplement.id,
                        animeTitle = animeDetail.title,
                        episodeTitle = episode.name,
                        imageUrl = animeDetail.images.webp.large_image_url,
                        number = episode.episodeNo,
                        isFiller = episode.filler,
                        servers = serversResponse.data,
                        sources = sourcesResponse.data,
                        sourcesQuery = query
                    )
                }
                if (complement != null) {
                    animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(complement)
                    _detailState.update {
                        it.copy(
                            episodeDetailComplements = it.episodeDetailComplements + (episodeId to Resource.Success(
                                complement
                            ))
                        )
                    }
                } else {
                    _detailState.update {
                        it.copy(
                            episodeDetailComplements = it.episodeDetailComplements + (episodeId to Resource.Error(
                                "Failed to load episode details"
                            ))
                        )
                    }
                }
            } else {
                _detailState.update {
                    it.copy(
                        episodeDetailComplements = it.episodeDetailComplements + (episodeId to Resource.Error(
                            "Failed to load episode servers or sources"
                        ))
                    )
                }
            }
        } finally {
            isLoadingEpisodeDetail = false
        }
    }

    private fun loadEpisodes(isRefresh: Boolean = false) = viewModelScope.launch {
        if (isLoadingEpisodes) {
            return@launch
        }
        isLoadingEpisodes = true
        try {
            _detailState.update { it.copy(animeDetailComplement = Resource.Loading()) }
            val detailData = _detailState.value.animeDetail.data?.data ?: run {
                _detailState.update { it.copy(animeDetailComplement = Resource.Error("Anime data not available")) }
                return@launch
            }
            if (handleCachedAnimeDetailComplement(detailData, isRefresh)) {
                return@launch
            }
            if (detailData.type == "Music") {
                val cachedAnimeDetailComplement = AnimeDetailComplement(
                    id = detailData.mal_id.toString(),
                    malId = detailData.mal_id,
                )
                animeEpisodeDetailRepository.insertCachedAnimeDetailComplement(
                    cachedAnimeDetailComplement
                )
                _detailState.update {
                    it.copy(
                        animeDetailComplement = Resource.Success(
                            cachedAnimeDetailComplement
                        )
                    )
                }
                return@launch
            }
            val searchTitle = (detailData.title_english ?: detailData.title).normalizeTitle()
            val response = animeEpisodeDetailRepository.getAnimeAniwatchSearch(searchTitle)
            if (!response.isSuccessful) {
                _detailState.update {
                    it.copy(
                        animeDetailComplement = Resource.Error(
                            response.errorBody()?.string() ?: "Unknown error"
                        )
                    )
                }
                return@launch
            }
            handleValidEpisode(response)
        } finally {
            isLoadingEpisodes = false
        }
    }

    private suspend fun handleCachedAnimeDetailComplement(
        detailData: AnimeDetail,
        isRefresh: Boolean
    ): Boolean {
        if (_detailState.value.animeDetailComplement is Resource.Success && !isRefresh) {
            return true
        }
        val cachedAnimeDetailComplement =
            animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(detailData.mal_id)
        cachedAnimeDetailComplement?.let { cachedAnimeDetail ->
            val updatedAnimeDetail =
                animeEpisodeDetailRepository.updateCachedAnimeDetailComplementWithEpisodes(
                    detailData,
                    cachedAnimeDetail,
                    isRefresh
                )
            if (updatedAnimeDetail == null) {
                _detailState.update { it.copy(animeDetailComplement = Resource.Error("Failed to fetch or update episodes")) }
                return true
            }
            _detailState.update {
                it.copy(
                    animeDetailComplement = Resource.Success(
                        updatedAnimeDetail
                    )
                )
            }
            _episodeFilterState.update {
                it.copy(
                    filteredEpisodes = updatedAnimeDetail.episodes?.reversed() ?: emptyList()
                )
            }
            updatedAnimeDetail.episodes?.firstOrNull()?.let { firstEpisode ->
                _detailState.update {
                    it.copy(defaultEpisodeId = firstEpisode.episodeId)
                }
            }
            return true
        }
        return false
    }

    private fun handleValidEpisode(response: Response<AnimeAniwatchSearchResponse>) =
        viewModelScope.launch {
            if (!response.isSuccessful || response.body() == null) {
                _detailState.update {
                    it.copy(
                        animeDetailComplement = Resource.Error(
                            response.errorBody()?.string() ?: "Unknown error"
                        )
                    )
                }
                return@launch
            }
            val animeDetail = _detailState.value.animeDetail.data?.data ?: run {
                _detailState.update { it.copy(animeDetailComplement = Resource.Error("Anime data not available")) }
                return@launch
            }
            val resultResponse = response.body()!!
            val targetTitles = listOfNotNull(
                animeDetail.title,
                animeDetail.title_english
            ) + (animeDetail.title_synonyms ?: emptyList())
            val animeAniwatchs = AnimeTitleFinder.findClosestMatches(
                targetTitles = targetTitles,
                data = resultResponse.animes,
                maxResults = 2,
                titleExtractor = { it.name }
            )
            if (animeAniwatchs.isEmpty()) {
                _detailState.update { it.copy(animeDetailComplement = Resource.Error("No episode found")) }
                return@launch
            }
            for (animeAniwatch in animeAniwatchs) {
                val animeId = animeAniwatch.id.substringBefore("?").trim()
                val episodesResponse = animeEpisodeDetailRepository.getEpisodes(animeId)
                if (episodesResponse !is Resource.Success) {
                    continue
                }
                val defaultEpisode = episodesResponse.data.episodes.firstOrNull()
                if (defaultEpisode == null) {
                    continue
                }

                val defaultEpisodeServersResponse =
                    getDefaultEpisodeServers(defaultEpisode.episodeId)
                if (defaultEpisodeServersResponse !is Resource.Success) {
                    continue
                }
                val defaultEpisodeSourcesResponse = StreamingUtils.getEpisodeSources(
                    defaultEpisodeServersResponse,
                    { id, server, category ->
                        animeEpisodeDetailRepository.getEpisodeSources(
                            id,
                            server,
                            category
                        )
                    }
                )
                if (defaultEpisodeSourcesResponse !is Resource.Success) {
                    continue
                }
                if (checkEpisodeSourceMalId(defaultEpisodeSourcesResponse)) {
                    val cachedAnimeDetailComplement = AnimeDetailComplement(
                        id = animeId,
                        malId = animeDetail.mal_id,
                        episodes = episodesResponse.data.episodes,
                        eps = animeAniwatch.episodes?.eps,
                        sub = animeAniwatch.episodes?.sub,
                        dub = animeAniwatch.episodes?.dub,
                    )
                    animeEpisodeDetailRepository.insertCachedAnimeDetailComplement(
                        cachedAnimeDetailComplement
                    )
                    _detailState.update {
                        it.copy(
                            animeDetailComplement = Resource.Success(
                                cachedAnimeDetailComplement
                            )
                        )
                    }
                    _episodeFilterState.update {
                        it.copy(
                            filteredEpisodes = cachedAnimeDetailComplement.episodes?.reversed()
                                ?: emptyList()
                        )
                    }
                    insertCachedEpisodeDetailComplement(
                        cachedAnimeDetailComplement.id,
                        defaultEpisode,
                        defaultEpisodeServersResponse,
                        defaultEpisodeSourcesResponse,
                        animeDetail
                    )
                    return@launch
                }
            }
            _detailState.update { it.copy(animeDetailComplement = Resource.Error("No episode found")) }
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
                    StreamingUtils.getEpisodeQuery(Resource.Success(servers), servers.episodeId)
                        ?.let { query ->
                            EpisodeDetailComplement(
                                id = episode.episodeId,
                                malId = animeDetail.mal_id,
                                aniwatchId = aniwatchId,
                                animeTitle = animeDetail.title,
                                episodeTitle = episode.name,
                                imageUrl = animeDetail.images.webp.large_image_url,
                                number = episode.episodeNo,
                                isFiller = episode.filler,
                                servers = servers,
                                sources = sources,
                                sourcesQuery = query
                            )
                        }
                if (cachedEpisodeDetailComplement != null) {
                    animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(
                        cachedEpisodeDetailComplement
                    )
                    _detailState.update {
                        it.copy(
                            defaultEpisodeId = cachedEpisodeDetailComplement.id,
                            episodeDetailComplements = it.episodeDetailComplements + (episode.episodeId to Resource.Success(
                                cachedEpisodeDetailComplement
                            ))
                        )
                    }
                }
            }
        }
    }

    private fun updateEpisodeQueryState(query: FilterUtils.EpisodeQueryState) =
        viewModelScope.launch {
            val episodes =
                _detailState.value.animeDetailComplement.data?.episodes?.reversed() ?: emptyList()
            _episodeFilterState.update {
                it.copy(
                    episodeQuery = query,
                    filteredEpisodes = FilterUtils.filterEpisodes(
                        episodes = episodes,
                        query = query,
                        episodeDetailComplements = _detailState.value.episodeDetailComplements
                    )
                )
            }
        }

    private fun handleToggleFavorite(favorite: Boolean) = viewModelScope.launch {
        _detailState.value.animeDetailComplement.data?.let {
            val updatedComplement = it.copy(isFavorite = favorite)
            _detailState.update { it.copy(animeDetailComplement = Resource.Success(updatedComplement)) }
            animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(updatedComplement)
        }
    }

    private suspend fun getDefaultEpisodeServers(defaultEpisodeId: String?): Resource<EpisodeServersResponse> =
        viewModelScope.async {
            defaultEpisodeId?.let { animeEpisodeDetailRepository.getEpisodeServers(it) }
                ?: Resource.Error("No default episode found")
        }.await()

    private fun checkEpisodeSourceMalId(response: Resource<EpisodeSourcesResponse>): Boolean {
        val result = _detailState.value.animeDetail.data?.data?.mal_id == response.data?.malID
        return result
    }
}