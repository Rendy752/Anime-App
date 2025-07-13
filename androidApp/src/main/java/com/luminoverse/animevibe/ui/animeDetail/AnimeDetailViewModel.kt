package com.luminoverse.animevibe.ui.animeDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.repository.LoadEpisodesResult
import com.luminoverse.animevibe.ui.main.SnackbarMessage
import com.luminoverse.animevibe.ui.main.SnackbarMessageType
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
        val currentComplement = _detailState.value.animeDetailComplement
        _detailState.update { it.copy(animeDetailComplement = Resource.Loading(data = currentComplement.data)) }

        val animeDetail = _detailState.value.animeDetail.data?.data ?: run {
            _detailState.update { it.copy(animeDetailComplement = Resource.Error("Anime data not available")) }
            return@launch
        }

        val result = animeEpisodeDetailRepository.loadAllEpisodes(
            animeDetail = animeDetail,
            isRefresh = isRefresh
        )

        when (result) {
            is LoadEpisodesResult.Success -> {
                if (result.newEpisodeIds.isNotEmpty()) {
                    val message = if (result.newEpisodeIds.size == 1) {
                        "1 new episode is available!"
                    } else {
                        "${result.newEpisodeIds.size} new episodes are available!"
                    }
                    _snackbarChannel.send(SnackbarMessage(message, SnackbarMessageType.INFO))

                    _detailState.update {
                        it.copy(
                            animeDetailComplement = Resource.Success(result.complement),
                            newEpisodeIdList = result.newEpisodeIds
                        )
                    }
                } else {
                    _detailState.update { it.copy(animeDetailComplement = Resource.Success(result.complement)) }
                    if (isRefresh) {
                        _snackbarChannel.send(
                            SnackbarMessage("No new episodes are available!", SnackbarMessageType.INFO)
                        )
                    }
                }
                _episodeFilterState.update {
                    it.copy(filteredEpisodes = result.complement.episodes?.reversed() ?: emptyList())
                }
            }
            is LoadEpisodesResult.Error -> {
                _detailState.update { it.copy(animeDetailComplement = Resource.Error(result.message)) }
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

    private fun handleToggleFavorite(isFavorite: Boolean) = viewModelScope.launch {
        val animeDetailData = _detailState.value.animeDetail.data?.data ?: return@launch
        val malId = animeDetailData.mal_id
        val complementId = _detailState.value.animeDetailComplement.data?.id

        val updatedComplement = animeEpisodeDetailRepository.toggleAnimeFavorite(
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