package com.luminoverse.animevibe.ui.animeWatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.repository.LoadEpisodesResult
import com.luminoverse.animevibe.ui.main.SnackbarMessage
import com.luminoverse.animevibe.ui.main.SnackbarMessageType
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class WatchState(
    val animeDetail: Resource<AnimeDetail> = Resource.Loading(),
    val animeDetailComplement: Resource<AnimeDetailComplement> = Resource.Loading(),
    val episodeDetailComplement: Resource<EpisodeDetailComplement> = Resource.Loading(),
    val episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>> = emptyMap(),
    val episodeSourcesQuery: EpisodeSourcesQuery? = null,
    val newEpisodeIdList: List<String> = emptyList(),
    val episodeJumpNumber: Int? = null,
    val isSideSheetVisible: Boolean = false,
    val isAutoplayNextEpisodeEnabled: Boolean = false,
    val errorSourceQueryList: List<EpisodeSourcesQuery> = emptyList(),
)

sealed class WatchAction {
    data class SetInitialState(val malId: Int, val episodeId: String) : WatchAction()
    data class HandleSelectedEpisodeServer(
        val episodeSourcesQuery: EpisodeSourcesQuery,
        val isRefresh: Boolean = false
    ) : WatchAction()

    data class LoadEpisodeDetailComplement(val episodeId: String) : WatchAction()
    data class UpdateLastEpisodeWatchedId(val lastEpisodeWatchedId: String) : WatchAction()
    data class UpdateStoredWatchState(
        val currentPosition: Long?, val duration: Long?, val screenShot: String?
    ) : WatchAction()

    data class AddErrorSourceQueryList(val errorSourceQueryList: EpisodeSourcesQuery) :
        WatchAction()

    data class SetEpisodeJumpNumber(val jumpNumber: Int) : WatchAction()
    data class SetSideSheetVisibility(val isVisible: Boolean) : WatchAction()
    data class SetAutoplayNextEpisodeEnabled(val isEnabled: Boolean) : WatchAction()
    data class ShowErrorMessage(val message: String) : WatchAction()
    data class SetFavorite(val isFavorite: Boolean) : WatchAction()
}

@HiltViewModel
class AnimeWatchViewModel @Inject constructor(
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository,
    val hlsPlayerUtils: HlsPlayerUtils,
    val networkDataSource: NetworkDataSource
) : ViewModel() {

    private val _watchState = MutableStateFlow(WatchState())
    val watchState: StateFlow<WatchState> = _watchState.asStateFlow()

    private val _snackbarChannel = Channel<SnackbarMessage>()
    val snackbarFlow = _snackbarChannel.receiveAsFlow()

    val playerCoreState: StateFlow<PlayerCoreState> = hlsPlayerUtils.playerCoreState
    val controlsState: StateFlow<ControlsState> = hlsPlayerUtils.controlsState

    fun getPlayer(): ExoPlayer? = hlsPlayerUtils.getPlayer()
    suspend fun captureScreenshot(): String? = hlsPlayerUtils.captureScreenshot()
    fun dispatchPlayerAction(action: HlsPlayerAction) = hlsPlayerUtils.dispatch(action)

    fun onAction(action: WatchAction) {
        when (action) {
            is WatchAction.SetInitialState -> setInitialState(action.malId, action.episodeId)
            is WatchAction.HandleSelectedEpisodeServer -> handleSelectedEpisodeServer(
                action.episodeSourcesQuery, action.isRefresh
            )

            is WatchAction.LoadEpisodeDetailComplement -> loadEpisodeDetailComplement(action.episodeId)
            is WatchAction.UpdateLastEpisodeWatchedId -> updateLastEpisodeWatchedId(action.lastEpisodeWatchedId)
            is WatchAction.UpdateStoredWatchState -> updateStoredWatchState(
                action.currentPosition,
                action.duration,
                action.screenShot
            )

            is WatchAction.AddErrorSourceQueryList -> _watchState.update {
                it.copy(errorSourceQueryList = it.errorSourceQueryList + action.errorSourceQueryList)
            }

            is WatchAction.SetEpisodeJumpNumber -> _watchState.update { it.copy(episodeJumpNumber = action.jumpNumber) }
            is WatchAction.SetSideSheetVisibility -> _watchState.update { it.copy(isSideSheetVisible = action.isVisible) }
            is WatchAction.SetAutoplayNextEpisodeEnabled -> _watchState.update {
                it.copy(
                    isAutoplayNextEpisodeEnabled = action.isEnabled
                )
            }

            is WatchAction.SetFavorite -> updateFavoriteInComplement(action.isFavorite)
            is WatchAction.ShowErrorMessage -> viewModelScope.launch {
                _snackbarChannel.send(SnackbarMessage(action.message, SnackbarMessageType.ERROR))
            }
        }
    }

    private fun setInitialState(malId: Int, episodeId: String) = viewModelScope.launch {
        _watchState.update {
            it.copy(
                episodeSourcesQuery = episodeSourcesQueryPlaceholder.copy(id = episodeId),
                episodeJumpNumber = if (_watchState.value.animeDetail.data?.mal_id == malId) _watchState.value.episodeJumpNumber else null,
                animeDetail = Resource.Loading(),
                animeDetailComplement = Resource.Loading(),
                episodeDetailComplement = Resource.Loading()
            )
        }

        val (animeDetailResource, isFromCache) = animeEpisodeDetailRepository.getAnimeDetail(malId)
        if (animeDetailResource !is Resource.Success) {
            _watchState.update { it.copy(animeDetail = Resource.Error("Failed to load anime details.")) }
            return@launch
        }
        _watchState.update { it.copy(animeDetail = Resource.Success(animeDetailResource.data.data)) }

        val episodeLoadResult = loadAllEpisodes()
        if (episodeLoadResult is LoadEpisodesResult.Success) {
            val complement = episodeLoadResult.complement
            val lastWatchedId = complement.lastEpisodeWatchedId
            val targetEpisodeId =
                episodeId.ifEmpty { lastWatchedId ?: complement.episodes?.firstOrNull()?.id }

            if (targetEpisodeId != null) {
                val initialQuery = determineInitialQuery(targetEpisodeId, complement)
                handleSelectedEpisodeServer(initialQuery, isRefresh = false)
            } else {
                onAction(WatchAction.ShowErrorMessage("No episodes available to play."))
            }
        }

        if (isFromCache) {
            animeEpisodeDetailRepository.getUpdatedAnimeDetailById(malId).let { updatedResource ->
                if (updatedResource is Resource.Success) {
                    _watchState.update { it.copy(animeDetail = Resource.Success(updatedResource.data.data)) }
                }
            }
        }
    }

    private suspend fun determineInitialQuery(
        episodeId: String,
        complement: AnimeDetailComplement
    ): EpisodeSourcesQuery {
        val cachedEpisode = animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId)
        if (cachedEpisode != null) return cachedEpisode.sourcesQuery.copy(id = episodeId)
        val allServers = complement.episodes?.find { it.id == episodeId }?.let { ep ->
            animeEpisodeDetailRepository.getEpisodeServers(ep.id).data?.results
        }
        if (allServers.isNullOrEmpty()) return episodeSourcesQueryPlaceholder.copy(id = episodeId)

        val lastSubServer = allServers.lastOrNull { it.type == "sub" }
        val priorityServer = lastSubServer ?: allServers.first()
        return EpisodeSourcesQuery.create(episodeId, priorityServer.serverName, priorityServer.type)
    }

    private fun handleSelectedEpisodeServer(
        episodeSourcesQuery: EpisodeSourcesQuery,
        isRefresh: Boolean = false
    ) = viewModelScope.launch {
        _watchState.update {
            val currentNewIds = it.newEpisodeIdList - episodeSourcesQuery.id
            it.copy(
                episodeDetailComplement = Resource.Loading(it.episodeDetailComplement.data),
                episodeSourcesQuery = episodeSourcesQuery,
                newEpisodeIdList = currentNewIds
            )
        }

        val result = animeEpisodeDetailRepository.getEpisodeStreamingDetails(
            episodeSourcesQuery = episodeSourcesQuery,
            isRefresh = isRefresh,
            errorSourceQueryList = _watchState.value.errorSourceQueryList,
            animeDetail = _watchState.value.animeDetail.data,
            animeDetailComplement = _watchState.value.animeDetailComplement.data
        )

        if (result is Resource.Success) {
            _watchState.update {
                it.copy(
                    episodeDetailComplement = result,
                    episodeSourcesQuery = result.data.sourcesQuery
                )
            }
        } else {
            _watchState.update {
                it.copy(
                    episodeDetailComplement = Resource.Error(
                        result.message ?: "Failed to load streaming details."
                    )
                )
            }
            onAction(
                WatchAction.ShowErrorMessage(
                    result.message ?: "Could not find a working server."
                )
            )
        }
    }

    /**
     * Refactored to be a suspend function that returns its result.
     * This removes the need for a callback and makes the control flow sequential.
     */
    private suspend fun loadAllEpisodes(): LoadEpisodesResult {
        val animeDetail = _watchState.value.animeDetail.data
            ?: return LoadEpisodesResult.Error("Anime Detail not available.")

        val result = animeEpisodeDetailRepository.loadAllEpisodes(
            animeDetail = animeDetail, isRefresh = true
        )

        when (result) {
            is LoadEpisodesResult.Success -> {
                if (result.newEpisodeIds.isNotEmpty()) {
                    _watchState.update {
                        it.copy(
                            animeDetailComplement = Resource.Success(result.complement),
                            newEpisodeIdList = (it.newEpisodeIdList + result.newEpisodeIds).distinct()
                        )
                    }
                } else {
                    _watchState.update { it.copy(animeDetailComplement = Resource.Success(result.complement)) }
                }
            }

            is LoadEpisodesResult.Error -> {
                _watchState.update { it.copy(animeDetailComplement = Resource.Error(result.message)) }
            }
        }
        return result
    }

    private fun updateLastEpisodeWatchedId(lastEpisodeWatchedId: String) = viewModelScope.launch {
        val complement = _watchState.value.animeDetailComplement.data ?: return@launch
        if (complement.lastEpisodeWatchedId == lastEpisodeWatchedId) return@launch
        val updatedComplement = complement.copy(lastEpisodeWatchedId = lastEpisodeWatchedId)
        animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(updatedComplement)
    }

    private fun updateStoredWatchState(
        currentPosition: Long?,
        duration: Long?,
        screenShot: String?
    ) = viewModelScope.launch {
        val episodeComplement = _watchState.value.episodeDetailComplement.data ?: return@launch
        val cachedEpisodeComplement =
            animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeComplement.id)
        val updatedComplement = cachedEpisodeComplement?.copy(
            lastTimestamp = currentPosition,
            duration = duration,
            screenshot = screenShot,
            lastWatched = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ) ?: episodeComplement.copy(
            lastTimestamp = currentPosition,
            duration = duration,
            screenshot = screenShot,
            lastWatched = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(updatedComplement)
    }

    private fun loadEpisodeDetailComplement(episodeId: String) = viewModelScope.launch {
        _watchState.update { it.copy(episodeDetailComplements = it.episodeDetailComplements + (episodeId to Resource.Loading())) }
        val cachedComplement =
            animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId)
        val result = if (cachedComplement != null) {
            Resource.Success(cachedComplement)
        } else {
            Resource.Error("Episode detail complement not found")
        }
        _watchState.update { it.copy(episodeDetailComplements = it.episodeDetailComplements + (episodeId to result)) }
    }

    private fun updateFavoriteInComplement(isFavorite: Boolean) = viewModelScope.launch {
        val complement = _watchState.value.episodeDetailComplement.data ?: return@launch
        animeEpisodeDetailRepository.toggleEpisodeFavorite(complement.id, isFavorite)
    }
}