package com.example.animeapp.ui.animeDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.*
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.utils.AnimeTitleFinder
import com.example.animeapp.utils.AnimeTitleFinder.normalizeTitle
import com.example.animeapp.utils.ComplementUtils
import com.example.animeapp.utils.FilterUtils
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.StreamingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
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
            onAction(DetailAction.LoadEpisodes())
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
        if (isLoadingEpisodeDetail) return@launch
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
            _detailState.update {
                it.copy(
                    episodeDetailComplements = it.episodeDetailComplements + (episodeId to Resource.Error(
                        "Episode complement not found"
                    ))
                )
            }
        } finally {
            isLoadingEpisodeDetail = false
        }
    }

    private fun loadEpisodes(isRefresh: Boolean = false) = viewModelScope.launch {
        if (isLoadingEpisodes) return@launch
        isLoadingEpisodes = true
        try {
            _detailState.update { it.copy(animeDetailComplement = Resource.Loading()) }
            val animeDetail = _detailState.value.animeDetail.data?.data ?: run {
                _detailState.update { it.copy(animeDetailComplement = Resource.Error("Anime data not available")) }
                return@launch
            }
            if (animeDetail.type == "Music") {
                val complement = ComplementUtils.getOrCreateAnimeDetailComplement(
                    repository = animeEpisodeDetailRepository,
                    malId = animeDetail.mal_id
                )
                if (complement != null) {
                    animeEpisodeDetailRepository.insertCachedAnimeDetailComplement(complement)
                    _detailState.update {
                        it.copy(animeDetailComplement = Resource.Success(complement))
                    }
                } else {
                    _detailState.update { it.copy(animeDetailComplement = Resource.Error("Failed to create anime complement")) }
                }
                return@launch
            }
            val cachedComplement =
                animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(animeDetail.mal_id)
            if (cachedComplement != null && !isRefresh) {
                val updatedComplement = ComplementUtils.updateAnimeDetailComplementWithEpisodes(
                    repository = animeEpisodeDetailRepository,
                    animeDetail = animeDetail,
                    animeDetailComplement = cachedComplement
                )
                if (updatedComplement != null) {
                    animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(updatedComplement)
                    updateStateWithComplement(updatedComplement)
                    return@launch
                }
            }
            val searchTitle = (animeDetail.title_english ?: animeDetail.title).normalizeTitle()
            val animeAniwatchSearchResponse =
                animeEpisodeDetailRepository.getAnimeAniwatchSearch(searchTitle)
            if (animeAniwatchSearchResponse !is Resource.Success) {
                _detailState.update {
                    it.copy(
                        animeDetailComplement = Resource.Error(
                            animeAniwatchSearchResponse.message ?: "Failed to search anime"
                        )
                    )
                }
                return@launch
            }
            handleValidEpisode(animeAniwatchSearchResponse, animeDetail)
        } finally {
            isLoadingEpisodes = false
        }
    }

    private suspend fun handleValidEpisode(
        response: Resource.Success<AnimeAniwatchSearchResponse>,
        animeDetail: AnimeDetail
    ) {
        val targetTitles = listOfNotNull(
            animeDetail.title,
            animeDetail.title_english
        ) + (animeDetail.title_synonyms ?: emptyList())
        val animeAniwatchs = AnimeTitleFinder.findClosestMatches(
            targetTitles = targetTitles,
            data = response.data.animes,
            maxResults = 2,
            titleExtractor = { it.name }
        )
        if (animeAniwatchs.isEmpty()) {
            _detailState.update { it.copy(animeDetailComplement = Resource.Error("No episode found")) }
            return
        }
        for (animeAniwatch in animeAniwatchs) {
            val animeId = animeAniwatch.id.substringBefore("?").trim()
            val episodesResponse = animeEpisodeDetailRepository.getEpisodes(animeId)
            if (episodesResponse !is Resource.Success) continue

            val defaultEpisode = episodesResponse.data.episodes.firstOrNull() ?: continue
            val defaultEpisodeServersResponse =
                animeEpisodeDetailRepository.getEpisodeServers(defaultEpisode.episodeId)
            if (defaultEpisodeServersResponse !is Resource.Success) continue

            val (defaultEpisodeSourcesResponse, defaultEpisodeSourcesQuery) = StreamingUtils.getEpisodeSources(
                defaultEpisodeServersResponse,
                { id, server, category ->
                    animeEpisodeDetailRepository.getEpisodeSources(id, server, category)
                }
            )
            if (defaultEpisodeSourcesResponse !is Resource.Success) continue
            if (checkEpisodeSourceMalId(defaultEpisodeSourcesResponse)) {
                val complement = ComplementUtils.getOrCreateAnimeDetailComplement(
                    repository = animeEpisodeDetailRepository,
                    id = animeId,
                    malId = animeDetail.mal_id
                )?.copy(
                    id = animeId,
                    episodes = episodesResponse.data.episodes,
                    eps = animeAniwatch.episodes?.eps,
                    sub = animeAniwatch.episodes?.sub,
                    dub = animeAniwatch.episodes?.dub
                )
                if (complement != null) {
                    animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(complement)
                    updateStateWithComplement(complement)
                    if (defaultEpisodeSourcesQuery != null) {
                        ComplementUtils.createEpisodeDetailComplement(
                            repository = animeEpisodeDetailRepository,
                            animeDetail = animeDetail,
                            animeDetailComplement = complement,
                            episode = defaultEpisode,
                            servers = defaultEpisodeServersResponse.data,
                            sources = defaultEpisodeSourcesResponse.data,
                            sourcesQuery = defaultEpisodeSourcesQuery
                        ).let { episodeComplement ->
                            _detailState.update {
                                it.copy(
                                    defaultEpisodeId = episodeComplement.id,
                                    episodeDetailComplements = it.episodeDetailComplements + (episodeComplement.id to Resource.Success(
                                        episodeComplement
                                    ))
                                )
                            }
                        }
                    }
                    return
                }
            }
        }
        _detailState.update { it.copy(animeDetailComplement = Resource.Error("No valid episode found")) }
    }

    private fun updateStateWithComplement(complement: AnimeDetailComplement) {
        _detailState.update { it.copy(animeDetailComplement = Resource.Success(complement)) }
        _episodeFilterState.update {
            it.copy(filteredEpisodes = complement.episodes?.reversed() ?: emptyList())
        }
        complement.episodes?.firstOrNull()?.let { firstEpisode ->
            _detailState.update { it.copy(defaultEpisodeId = firstEpisode.episodeId) }
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

    private fun handleToggleFavorite(isFavorite: Boolean) = viewModelScope.launch {
        _detailState.value.animeDetail.data?.data?.mal_id?.let { malId ->
            val updatedComplement = ComplementUtils.toggleAnimeFavorite(
                repository = animeEpisodeDetailRepository,
                id = _detailState.value.animeDetailComplement.data?.id,
                malId = malId,
                isFavorite = isFavorite
            )
            if (updatedComplement != null) {
                animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(updatedComplement)
                _detailState.update {
                    it.copy(
                        animeDetailComplement = Resource.Success(
                            updatedComplement
                        )
                    )
                }
            } else {
                _detailState.update { it.copy(animeDetailComplement = Resource.Error("Failed to update favorite status")) }
            }
        }
    }

    private fun checkEpisodeSourceMalId(response: Resource<EpisodeSourcesResponse>): Boolean =
        _detailState.value.animeDetail.data?.data?.mal_id == response.data?.malID
}