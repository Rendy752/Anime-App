package com.luminoverse.animevibe.ui.animeDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.ui.main.SnackbarMessage
import com.luminoverse.animevibe.ui.main.SnackbarMessageType
import com.luminoverse.animevibe.utils.watch.AnimeTitleFinder.normalizeTitle
import com.luminoverse.animevibe.utils.ComplementUtils
import com.luminoverse.animevibe.utils.FilterUtils
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.workers.WorkerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailState(
    val animeDetail: Resource<AnimeDetailResponse> = Resource.Loading(),
    val animeDetailComplement: Resource<AnimeDetailComplement?> = Resource.Loading(),
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
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository,
    private val workerScheduler: WorkerScheduler
) : ViewModel() {

    private val _detailState = MutableStateFlow(DetailState())
    val detailState: StateFlow<DetailState> = _detailState.asStateFlow()

    private val _episodeFilterState = MutableStateFlow(EpisodeFilterState())
    val episodeFilterState: StateFlow<EpisodeFilterState> = _episodeFilterState.asStateFlow()

    private val _snackbarChannel = Channel<SnackbarMessage>()
    val snackbarFlow = _snackbarChannel.receiveAsFlow()

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
        val (result, isFromCache) = animeEpisodeDetailRepository.getAnimeDetail(id)
        _detailState.update { it.copy(animeDetail = result) }
        if (result !is Resource.Success) return@launch
        if (isFromCache) {
            val updatedAnimeDetail = animeEpisodeDetailRepository.getUpdatedAnimeDetailById(id)
            if (updatedAnimeDetail is Resource.Success) {
                _detailState.update { it.copy(animeDetail = updatedAnimeDetail) }
            }
        }
        onAction(DetailAction.LoadAllEpisode())
    }

    private fun loadRelationAnimeDetail(id: Int) = viewModelScope.launch {
        _detailState.update {
            it.copy(relationAnimeDetails = it.relationAnimeDetails + (id to Resource.Loading()))
        }
        val (result, _) = animeEpisodeDetailRepository.getAnimeDetail(id)
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

        if (animeDetail.type == "Music") return@launch handleUnavailableEpisode(animeDetail.mal_id)
        if (handleCachedComplement(animeDetail, isRefresh) &&
            isRefresh && isCurrentAnimeDetailComplement.data?.id?.all { it.isDigit() } == false
        ) {
            detectNewEpisodes(animeDetail, isCurrentAnimeDetailComplement)
            return@launch
        }

        handleUnregisteredEpisode(
            animeMalId = animeDetail.mal_id,
            animeEnglishTitle = animeDetail.title_english,
            animeTitle = animeDetail.title,
        )
    }

    private suspend fun handleCachedComplement(
        animeDetail: AnimeDetail, isRefresh: Boolean
    ): Boolean {
        val cachedComplement =
            animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(animeDetail.mal_id)
        cachedComplement?.let {
            val updatedComplement = ComplementUtils.updateAnimeDetailComplementWithEpisodes(
                repository = animeEpisodeDetailRepository,
                animeDetail = animeDetail,
                animeDetailComplement = it,
                isRefresh = isRefresh
            )
            if (updatedComplement != null) {
                updateSuccessAdditionalState(updatedComplement)
                return true
            }
        }
        return false
    }

    private suspend fun detectNewEpisodes(
        animeDetail: AnimeDetail,
        isCurrentAnimeDetailComplement: Resource<AnimeDetailComplement?>
    ) {
        isCurrentAnimeDetailComplement.data?.episodes?.let { currentEpisodes ->
            val currentEpisodeIds = currentEpisodes.map { it.id }.toSet()
            val cachedComplement =
                animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(
                    animeDetail.mal_id
                )
            cachedComplement?.episodes?.let { cachedEpisodes ->
                val newEpisodeIds = cachedEpisodes
                    .filter { episode -> episode.id !in currentEpisodeIds }
                    .map { episode -> episode.id }

                if (newEpisodeIds.isEmpty()) {
                    _detailState.update { it.copy(newEpisodeIdList = emptyList()) }
                    _snackbarChannel.send(
                        SnackbarMessage(
                            message = "No new episodes are available!",
                            type = SnackbarMessageType.INFO
                        )
                    )
                    return@let
                }
                val message = if (newEpisodeIds.size == 1) {
                    "1 new episode is available!"
                } else {
                    "${newEpisodeIds.size} new episodes are available!"
                }
                _snackbarChannel.send(
                    SnackbarMessage(
                        message = message,
                        type = SnackbarMessageType.INFO
                    )
                )
                _detailState.update { it.copy(newEpisodeIdList = newEpisodeIds) }
            }
        }
    }

    private fun handleUnavailableEpisode(malId: Int) {
        viewModelScope.launch {
            val complement = ComplementUtils.getOrCreateAnimeDetailComplement(
                repository = animeEpisodeDetailRepository,
                malId = malId
            )
            if (complement != null) {
                _detailState.update { it.copy(animeDetailComplement = Resource.Success(complement)) }
            } else {
                _detailState.update { it.copy(animeDetailComplement = Resource.Error("Failed to create anime complement")) }
            }
        }
    }

    private suspend fun handleUnregisteredEpisode(
        animeMalId: Int,
        animeEnglishTitle: String?,
        animeTitle: String,
    ) {
        val searchTitles = listOfNotNull(animeEnglishTitle, animeTitle).distinct()

        var relatedAnime: AnimeAniwatch? = null

        for (title in searchTitles) {
            val searchResponse =
                animeEpisodeDetailRepository.getAnimeAniwatchSearch(title.normalizeTitle())

            when (searchResponse) {
                is Resource.Success -> {
                    val foundAnime =
                        searchResponse.data.results.data.find { it.malID == animeMalId }
                    if (foundAnime != null) {
                        relatedAnime = foundAnime
                        break
                    }
                }

                is Resource.Error -> {
                    _detailState.update {
                        it.copy(
                            animeDetailComplement = Resource.Error(
                                searchResponse.message
                            )
                        )
                    }
                    return
                }

                is Resource.Loading<*> -> {
                    continue
                }
            }
        }

        val finalRelatedAnime = relatedAnime ?: run {
            handleUnavailableEpisode(animeMalId)
            return
        }

        val episodesResource = animeEpisodeDetailRepository.getEpisodes(finalRelatedAnime.id)

        val complement = ComplementUtils.getOrCreateAnimeDetailComplement(
            repository = animeEpisodeDetailRepository,
            id = finalRelatedAnime.id,
            malId = animeMalId
        )?.copy(
            id = finalRelatedAnime.id,
            episodes = if (episodesResource is Resource.Success) episodesResource.data.results.episodes else null,
            eps = finalRelatedAnime.tvInfo.eps,
            sub = finalRelatedAnime.tvInfo.sub,
            dub = finalRelatedAnime.tvInfo.dub
        )
        if (complement != null) {
            animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(complement)
            updateSuccessAdditionalState(complement)
        }
    }

    private fun updateSuccessAdditionalState(complement: AnimeDetailComplement) {
        _detailState.update { it.copy(animeDetailComplement = Resource.Success(complement)) }
        _episodeFilterState.update {
            it.copy(filteredEpisodes = complement.episodes?.reversed() ?: emptyList())
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
        val animeDetailData = _detailState.value.animeDetail.data?.data ?: return@launch
        val malId = animeDetailData.mal_id
        val complementId = _detailState.value.animeDetailComplement.data?.id

        val updatedComplement = ComplementUtils.toggleAnimeFavorite(
            repository = animeEpisodeDetailRepository,
            id = complementId,
            malId = malId,
            isFavorite = isFavorite
        )

        if (updatedComplement != null) {
            _detailState.update {
                it.copy(
                    animeDetailComplement = Resource.Success(updatedComplement)
                )
            }
            if (!animeDetailData.airing) return@launch
            if (isFavorite) {
                workerScheduler.scheduleImmediateBroadcastNotification(animeDetailData)
            } else {
                workerScheduler.cancelImmediateBroadcastNotification(malId)
            }
        } else {
            _detailState.update { it.copy(animeDetailComplement = Resource.Error("Failed to update favorite status")) }
        }
    }
}