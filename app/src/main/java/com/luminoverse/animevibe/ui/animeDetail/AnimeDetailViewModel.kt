package com.luminoverse.animevibe.ui.animeDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.utils.watch.AnimeTitleFinder
import com.luminoverse.animevibe.utils.watch.AnimeTitleFinder.normalizeTitle
import com.luminoverse.animevibe.utils.ComplementUtils
import com.luminoverse.animevibe.utils.FilterUtils
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.media.StreamingUtils
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
    val newEpisodeIdList: List<String> = emptyList(),
    val relationAnimeDetails: Map<Int, Resource<AnimeDetail>> = emptyMap(),
    val episodeDetailComplements: Map<String, EpisodeDetailComplement?> = emptyMap()
)

data class EpisodeFilterState(
    val episodeQuery: FilterUtils.EpisodeQueryState = FilterUtils.EpisodeQueryState(),
    val filteredEpisodes: List<Episode> = emptyList()
)

sealed class DetailAction {
    data class LoadAnimeDetail(val id: Int) : DetailAction()
    data class LoadRelationAnimeDetail(val id: Int) : DetailAction()
    data class LoadEpisodeDetail(val episodeId: String) : DetailAction()
    data class LoadAllEpisode(val isRefresh: Boolean = false) : DetailAction()
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

    private var isLoadingEpisodeDetail = false

    fun onAction(action: DetailAction) {
        when (action) {
            is DetailAction.LoadAnimeDetail -> loadAnimeDetail(action.id)
            is DetailAction.LoadRelationAnimeDetail -> loadRelationAnimeDetail(action.id)
            is DetailAction.LoadEpisodeDetail -> loadEpisodeDetail(action.episodeId)
            is DetailAction.LoadAllEpisode -> loadAllEpisode(action.isRefresh)
            is DetailAction.UpdateEpisodeQueryState -> updateEpisodeQueryState(action.query)
            is DetailAction.ToggleFavorite -> handleToggleFavorite(action.isFavorite)
        }
    }

    private fun loadAnimeDetail(id: Int) = viewModelScope.launch {
        _detailState.update { it.copy(animeDetail = Resource.Loading()) }
        val result = animeEpisodeDetailRepository.getAnimeDetail(id)
        _detailState.update { it.copy(animeDetail = result) }
        if (result is Resource.Success) {
            onAction(DetailAction.LoadAllEpisode())
        }
    }

    private fun loadRelationAnimeDetail(id: Int) = viewModelScope.launch {
        _detailState.update {
            it.copy(relationAnimeDetails = it.relationAnimeDetails + (id to Resource.Loading()))
        }
        val result = animeEpisodeDetailRepository.getAnimeDetail(id)
        val animeDetail = when (result) {
            is Resource.Success -> Resource.Success(result.data.data)
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
        _detailState.update {
            it.copy(relationAnimeDetails = it.relationAnimeDetails + (id to animeDetail))
        }
    }

    private fun loadEpisodeDetail(episodeId: String) = viewModelScope.launch {
        if (isLoadingEpisodeDetail) return@launch
        isLoadingEpisodeDetail = true
        _detailState.update {
            it.copy(episodeDetailComplements = it.episodeDetailComplements + (episodeId to null))
        }
        try {
            val cachedComplement =
                animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId)
            if (cachedComplement != null) {
                _detailState.update {
                    it.copy(
                        episodeDetailComplements = it.episodeDetailComplements.toMutableMap()
                            .apply { this[episodeId] = cachedComplement }
                    )
                }
                return@launch
            }
        } finally {
            isLoadingEpisodeDetail = false
        }
    }

    private fun loadAllEpisode(isRefresh: Boolean = false) = viewModelScope.launch {
        val isCurrentAnimeDetailComplement = _detailState.value.animeDetailComplement
        _detailState.update { it.copy(animeDetailComplement = Resource.Loading(data = isCurrentAnimeDetailComplement.data)) }
        val animeDetail = _detailState.value.animeDetail.data?.data ?: run {
            _detailState.update { it.copy(animeDetailComplement = Resource.Error("Anime data not available")) }
            return@launch
        }

        if (animeDetail.type == "Music") return@launch handleMusicAnimeType(animeDetail)
        if (handleCachedComplement(animeDetail, isRefresh)) return@launch
        if (handleNewEpisodes(animeDetail, isCurrentAnimeDetailComplement)) return@launch

        handleUnregisteredEpisode(
            animeMalId = animeDetail.mal_id,
            animeImageUrl = animeDetail.images.webp.large_image_url,
            animeEnglishTitle = animeDetail.title_english,
            animeTitle = animeDetail.title,
            animeTitleSynonyms = animeDetail.title_synonyms
        )
    }

    private suspend fun handleCachedComplement(
        animeDetail: AnimeDetail, isRefresh: Boolean
    ): Boolean {
        val cachedComplement =
            animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(animeDetail.mal_id)
        if (cachedComplement != null && !isRefresh) {
            val updatedComplement = ComplementUtils.updateAnimeDetailComplementWithEpisodes(
                repository = animeEpisodeDetailRepository,
                animeDetail = animeDetail,
                animeDetailComplement = cachedComplement
            )
            if (updatedComplement != null) {
                updateSuccessAdditionalState(updatedComplement)
                return true
            }
        }
        return false
    }

    private suspend fun handleNewEpisodes(
        animeDetail: AnimeDetail,
        isCurrentAnimeDetailComplement: Resource<AnimeDetailComplement?>
    ): Boolean {
        isCurrentAnimeDetailComplement.data?.episodes?.let { currentEpisodes ->
            val currentEpisodeIds = currentEpisodes.map { it.episodeId }.toSet()
            val cachedComplement =
                animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(
                    animeDetail.mal_id
                )
            cachedComplement?.let {
                val updatedComplement = ComplementUtils.updateAnimeDetailComplementWithEpisodes(
                    repository = animeEpisodeDetailRepository,
                    animeDetail = animeDetail,
                    animeDetailComplement = it,
                    isRefresh = true
                )
                val newEpisodes = updatedComplement?.episodes ?: emptyList()
                val newEpisodeIds = newEpisodes
                    .filter { episode -> episode.episodeId !in currentEpisodeIds }
                    .map { episode -> episode.episodeId }
                _detailState.update { state ->
                    state.copy(
                        animeDetailComplement = Resource.Success(updatedComplement),
                        newEpisodeIdList = newEpisodeIds
                    )
                }
            }
            return true
        }
        return false
    }

    private fun handleMusicAnimeType(animeDetail: AnimeDetail) {
        viewModelScope.launch {
            val complement = ComplementUtils.getOrCreateAnimeDetailComplement(
                repository = animeEpisodeDetailRepository,
                malId = animeDetail.mal_id
            )
            if (complement != null) {
                _detailState.update {
                    it.copy(animeDetailComplement = Resource.Success(complement))
                }
            } else {
                _detailState.update { it.copy(animeDetailComplement = Resource.Error("Failed to create anime complement")) }
            }
        }
    }

    private suspend fun handleUnregisteredEpisode(
        animeMalId: Int,
        animeImageUrl: String?,
        animeEnglishTitle: String?,
        animeTitle: String,
        animeTitleSynonyms: List<String>?
    ) {
        val searchTitle = (animeEnglishTitle ?: animeTitle).normalizeTitle()
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
            return
        }

        val targetTitles = listOfNotNull(
            animeTitle,
            animeEnglishTitle
        ) + (animeTitleSynonyms ?: emptyList())
        val animeAniwatchs = AnimeTitleFinder.findClosestMatches(
            targetTitles = targetTitles,
            data = animeAniwatchSearchResponse.data.animes,
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

            val (defaultEpisodeSourcesResponse, defaultEpisodeSourcesQuery) = StreamingUtils.getEpisodeSourcesResult(
                episodeServersResponse = defaultEpisodeServersResponse.data,
                getEpisodeSources = { id, server, category ->
                    animeEpisodeDetailRepository.getEpisodeSources(id, server, category)
                }
            )
            if (defaultEpisodeSourcesResponse !is Resource.Success) continue
            if (checkEpisodeSourceMalId(defaultEpisodeSourcesResponse)) {
                val complement = ComplementUtils.getOrCreateAnimeDetailComplement(
                    repository = animeEpisodeDetailRepository,
                    id = animeId,
                    malId = animeMalId
                )?.copy(
                    id = animeId,
                    episodes = episodesResponse.data.episodes,
                    eps = animeAniwatch.episodes?.eps,
                    sub = animeAniwatch.episodes?.sub,
                    dub = animeAniwatch.episodes?.dub
                )
                if (complement != null) {
                    animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(complement)
                    updateSuccessAdditionalState(complement)
                    if (defaultEpisodeSourcesQuery != null) {
                        ComplementUtils.createEpisodeDetailComplement(
                            repository = animeEpisodeDetailRepository,
                            animeDetailMalId = animeMalId,
                            animeDetailTitle = animeTitle,
                            animeDetailImageUrl = animeImageUrl,
                            animeDetailComplement = complement,
                            episode = defaultEpisode,
                            servers = defaultEpisodeServersResponse.data,
                            sources = defaultEpisodeSourcesResponse.data,
                            sourcesQuery = defaultEpisodeSourcesQuery
                        ).let { episodeComplement ->
                            _detailState.update {
                                it.copy(
                                    defaultEpisodeId = episodeComplement.id,
                                    episodeDetailComplements = it.episodeDetailComplements + (episodeComplement.id to episodeComplement)
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

    private fun updateSuccessAdditionalState(complement: AnimeDetailComplement) {
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