package com.luminoverse.animevibe.ui.animeWatch

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.AnimeDetailComplement
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.episodeSourcesQueryPlaceholder
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.utils.ComplementUtils
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.media.PositionState
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.media.StreamingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class WatchState(
    val animeDetail: AnimeDetail? = null,
    val animeDetailComplement: AnimeDetailComplement? = null,
    val episodeDetailComplement: EpisodeDetailComplement? = null,
    val episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>> = emptyMap(),
    val episodeSourcesQuery: EpisodeSourcesQuery? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val newEpisodeCount: Int = 0
)

@Stable
data class PlayerUiState(
    val errorSourceQueryList: List<EpisodeSourcesQuery> = emptyList(),
    val isFullscreen: Boolean = false,
    val isPipMode: Boolean = false,
    val isShowResume: Boolean = false,
    val isShowNextEpisode: Boolean = false
)

sealed class WatchAction {
    data class SetInitialState(val malId: Int, val episodeId: String) : WatchAction()
    data class HandleSelectedEpisodeServer(
        val episodeSourcesQuery: EpisodeSourcesQuery,
        val isFirstInit: Boolean = false,
        val isRefresh: Boolean = false
    ) : WatchAction()

    data class UpdateLastEpisodeWatchedId(val lastEpisodeWatchedId: String) : WatchAction()
    data class UpdateStoredWatchState(
        val currentPosition: Long?, val duration: Long?, val screenShot: String?
    ) : WatchAction()

    data class LoadEpisodeDetailComplement(val episodeId: String) : WatchAction()
    data class AddErrorSourceQueryList(val errorSourceQueryList: EpisodeSourcesQuery) :
        WatchAction()

    data class SetFullscreen(val isFullscreen: Boolean) : WatchAction()
    data class SetPipMode(val isPipMode: Boolean) : WatchAction()
    data class SetShowResume(val isShow: Boolean) : WatchAction()
    data class SetShowNextEpisode(val isShow: Boolean) : WatchAction()

    data class SetErrorMessage(val message: String?) : WatchAction()
    data class SetFavorite(val isFavorite: Boolean, val updateComplement: Boolean = true) :
        WatchAction()
}

@HiltViewModel
class AnimeWatchViewModel @Inject constructor(
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository,
    val hlsPlayerUtils: HlsPlayerUtils
) : ViewModel() {

    private val _watchState = MutableStateFlow(WatchState())
    val watchState: StateFlow<WatchState> = _watchState.asStateFlow()

    private val _playerUiState = MutableStateFlow(PlayerUiState())
    val playerUiState: StateFlow<PlayerUiState> = _playerUiState.asStateFlow()

    val playerCoreState: StateFlow<PlayerCoreState> = hlsPlayerUtils.playerCoreState
    val controlsState: StateFlow<ControlsState> = hlsPlayerUtils.controlsState
    val positionState: StateFlow<PositionState> = hlsPlayerUtils.positionState

    private val _defaultEpisodeDetailComplement = MutableStateFlow<EpisodeDetailComplement?>(null)

    init {
        viewModelScope.launch {
            hlsPlayerUtils.playerCoreState.collect { coreState ->
                if (coreState.error != null) {
                    onAction(
                        WatchAction.SetErrorMessage(
                            coreState.error.message ?: "Unknown player error"
                        )
                    )
                }

                when (coreState.playbackState) {
                    Player.STATE_ENDED -> {
                        _playerUiState.update {
                            it.copy(isShowNextEpisode = true)
                        }
                        onAction(WatchAction.SetErrorMessage(null))
                    }

                    Player.STATE_READY -> {
                        onAction(WatchAction.SetErrorMessage(null))
                    }

                    Player.STATE_BUFFERING -> {
                        if (coreState.error == null) {
                            onAction(WatchAction.SetErrorMessage(null))
                        }
                    }
                }
            }
        }
    }

    fun getPlayer(): ExoPlayer? {
        return hlsPlayerUtils.getPlayer()
    }

    fun dispatchPlayerAction(action: HlsPlayerAction) {
        hlsPlayerUtils.dispatch(action)
    }

    fun onAction(action: WatchAction) {
        when (action) {
            is WatchAction.SetInitialState -> setInitialState(action.malId, action.episodeId)
            is WatchAction.HandleSelectedEpisodeServer -> handleSelectedEpisodeServer(
                action.episodeSourcesQuery, action.isFirstInit, action.isRefresh
            )

            is WatchAction.UpdateLastEpisodeWatchedId -> updateLastEpisodeWatchedId(action.lastEpisodeWatchedId)
            is WatchAction.UpdateStoredWatchState -> updateStoredWatchState(
                action.currentPosition, action.duration, action.screenShot
            )

            is WatchAction.LoadEpisodeDetailComplement -> loadEpisodeDetailComplement(action.episodeId)

            is WatchAction.AddErrorSourceQueryList -> _playerUiState.update {
                it.copy(errorSourceQueryList = it.errorSourceQueryList + action.errorSourceQueryList)
            }

            is WatchAction.SetFullscreen -> _playerUiState.update { it.copy(isFullscreen = action.isFullscreen) }
            is WatchAction.SetPipMode -> _playerUiState.update { it.copy(isPipMode = action.isPipMode) }
            is WatchAction.SetShowResume -> _playerUiState.update {
                it.copy(isShowResume = action.isShow)
            }

            is WatchAction.SetShowNextEpisode -> _playerUiState.update { it.copy(isShowNextEpisode = action.isShow) }
            is WatchAction.SetErrorMessage -> _watchState.update { it.copy(errorMessage = action.message) }

            is WatchAction.SetFavorite -> {
                if (action.updateComplement) {
                    updateFavoriteInComplement(action.isFavorite)
                }
            }
        }
    }

    private fun setInitialState(malId: Int, episodeId: String) {
        viewModelScope.launch {
            val animeDetail = animeEpisodeDetailRepository.getCachedAnimeDetailById(malId)
            val animeDetailComplement = ComplementUtils.getOrCreateAnimeDetailComplement(
                repository = animeEpisodeDetailRepository,
                malId = malId
            )
            _watchState.update {
                it.copy(animeDetail = animeDetail, animeDetailComplement = animeDetailComplement)
            }

            animeDetailComplement?.let { complement ->
                val defaultEpisode = if (complement.lastEpisodeWatchedId != null) {
                    getCachedEpisodeDetailComplement(complement.lastEpisodeWatchedId)
                } else {
                    animeEpisodeDetailRepository.getCachedDefaultEpisodeDetailComplementByMalId(
                        malId
                    )
                }
                _defaultEpisodeDetailComplement.value = defaultEpisode
                val initialQuery = defaultEpisode?.sourcesQuery?.copy(id = episodeId)
                    ?: EpisodeSourcesQuery(
                        id = episodeId,
                        server = defaultEpisode?.sourcesQuery?.server
                            ?: episodeSourcesQueryPlaceholder.server,
                        category = defaultEpisode?.sourcesQuery?.category
                            ?: episodeSourcesQueryPlaceholder.category
                    )
                onAction(WatchAction.HandleSelectedEpisodeServer(initialQuery, isFirstInit = true))
            }
        }
    }

    private fun handleSelectedEpisodeServer(
        episodeSourcesQuery: EpisodeSourcesQuery,
        isFirstInit: Boolean = false,
        isRefresh: Boolean = false
    ) = viewModelScope.launch {
        try {
            _watchState.update {
                it.copy(
                    isRefreshing = true,
                    episodeSourcesQuery = episodeSourcesQuery,
                )
            }
            if (!isRefresh) {
                val cachedEpisodeDetailComplement =
                    getCachedEpisodeDetailComplement(episodeSourcesQuery.id)
                if (cachedEpisodeDetailComplement != null) {
                    if (cachedEpisodeDetailComplement.sourcesQuery == episodeSourcesQuery || isFirstInit) {
                        _watchState.update {
                            it.copy(
                                episodeSourcesQuery = cachedEpisodeDetailComplement.sourcesQuery,
                                episodeDetailComplement = cachedEpisodeDetailComplement,
                                errorMessage = null
                            )
                        }
                        return@launch
                    } else {
                        val episodeServersResource =
                            Resource.Success(cachedEpisodeDetailComplement.servers)
                        val (episodeSourcesResource, availableEpisodeSourcesQuery) = StreamingUtils.getEpisodeSourcesResult(
                            episodeServersResponse = episodeServersResource.data,
                            getEpisodeSources = { id, server, category ->
                                animeEpisodeDetailRepository.getEpisodeSources(id, server, category)
                            },
                            errorSourceQueryList = _playerUiState.value.errorSourceQueryList,
                            episodeSourcesQuery = episodeSourcesQuery
                        )
                        if (episodeSourcesResource !is Resource.Success) {
                            _watchState.update {
                                it.copy(errorMessage = "Failed to fetch episode sources.")
                            }
                            return@launch
                        }
                        episodeServersResource.data.let { servers ->
                            episodeSourcesResource.data.let { sources ->
                                val updatedEpisodeDetailComplement =
                                    cachedEpisodeDetailComplement.copy(
                                        servers = servers,
                                        sources = sources,
                                        sourcesQuery = availableEpisodeSourcesQuery
                                            ?: episodeSourcesQuery
                                    )
                                animeEpisodeDetailRepository.updateEpisodeDetailComplement(
                                    updatedEpisodeDetailComplement
                                )
                                _watchState.update {
                                    it.copy(
                                        episodeSourcesQuery = availableEpisodeSourcesQuery
                                            ?: episodeSourcesQuery,
                                        episodeDetailComplement = updatedEpisodeDetailComplement,
                                        errorMessage = null
                                    )
                                }
                                return@launch
                            }
                        }
                    }
                }
            }

            _watchState.value.animeDetail?.let { animeDetail ->
                _watchState.value.animeDetailComplement?.let { animeDetailComplement ->
                    if (animeDetail.airing && isRefresh) {
                        viewModelScope.launch { updateEpisodes() }
                    }
                    val episodeServersResource =
                        animeEpisodeDetailRepository.getEpisodeServers(episodeSourcesQuery.id)
                    if (episodeServersResource !is Resource.Success) {
                        _watchState.update {
                            it.copy(errorMessage = "Failed to fetch episode servers.")
                        }
                        return@launch
                    }

                    val (episodeSourcesResource, availableEpisodeSourcesQuery) = StreamingUtils.getEpisodeSourcesResult(
                        episodeServersResponse = episodeServersResource.data,
                        getEpisodeSources = { id, server, category ->
                            animeEpisodeDetailRepository.getEpisodeSources(id, server, category)
                        },
                        errorSourceQueryList = _playerUiState.value.errorSourceQueryList,
                        episodeSourcesQuery = episodeSourcesQuery
                    )
                    if (episodeSourcesResource is Resource.Success && availableEpisodeSourcesQuery != null) {
                        episodeServersResource.data.let { servers ->
                            episodeSourcesResource.data.let { sources ->
                                var cachedEpisodeDetailComplement =
                                    getCachedEpisodeDetailComplement(episodeSourcesQuery.id)
                                if (cachedEpisodeDetailComplement != null) {
                                    cachedEpisodeDetailComplement =
                                        cachedEpisodeDetailComplement.copy(
                                            servers = servers,
                                            sources = sources,
                                            sourcesQuery = availableEpisodeSourcesQuery
                                        )
                                    animeEpisodeDetailRepository.updateEpisodeDetailComplement(
                                        cachedEpisodeDetailComplement
                                    )
                                    _watchState.update {
                                        it.copy(
                                            episodeDetailComplement = cachedEpisodeDetailComplement,
                                        )
                                    }
                                } else {
                                    val currentEpisode =
                                        animeDetailComplement.episodes?.firstOrNull { it.episodeId == servers.episodeId }
                                    currentEpisode?.let { episode ->
                                        ComplementUtils.createEpisodeDetailComplement(
                                            repository = animeEpisodeDetailRepository,
                                            animeDetailMalId = animeDetail.mal_id,
                                            animeDetailTitle = animeDetail.title,
                                            animeDetailImageUrl = animeDetail.images.webp.large_image_url,
                                            animeDetailComplement = animeDetailComplement,
                                            episode = episode,
                                            servers = servers,
                                            sources = sources,
                                            sourcesQuery = availableEpisodeSourcesQuery
                                        ).let { remoteEpisodeDetailComplement ->
                                            _watchState.update {
                                                it.copy(
                                                    episodeDetailComplement = remoteEpisodeDetailComplement,
                                                )
                                            }
                                        }
                                    }
                                }
                                _watchState.update {
                                    it.copy(
                                        episodeSourcesQuery = availableEpisodeSourcesQuery,
                                        errorMessage = null
                                    )
                                }
                                return@launch
                            }
                        }
                    }
                    _watchState.update {
                        it.copy(
                            errorMessage = "Failed to fetch episode sources."
                        )
                    }
                }
            }
        } catch (e: Exception) {
            restoreDefaultValues()
            _watchState.update {
                it.copy(errorMessage = e.message ?: "An unexpected error occurred")
            }
        } finally {
            _watchState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun updateEpisodes() = viewModelScope.launch {
        try {
            _watchState.value.animeDetail?.let { animeDetail ->
                if (animeDetail.type == "Music") return@launch
                _watchState.value.animeDetailComplement?.let { animeDetailComplement ->
                    val currentEpisodeCount = animeDetailComplement.episodes?.size ?: 0
                    val cachedComplement =
                        animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(
                            animeDetail.mal_id
                        )
                    cachedComplement?.let {
                        val updatedComplement =
                            ComplementUtils.updateAnimeDetailComplementWithEpisodes(
                                repository = animeEpisodeDetailRepository,
                                animeDetail = animeDetail,
                                animeDetailComplement = it,
                                isRefresh = true
                            )
                        val newEpisodeCount = updatedComplement?.episodes?.size ?: 0
                        _watchState.update {
                            it.copy(
                                animeDetailComplement = updatedComplement,
                                newEpisodeCount = newEpisodeCount - currentEpisodeCount
                            )
                        }
                    }
                }
            }
        } finally {
        }
    }

    private fun updateLastEpisodeWatchedId(lastEpisodeWatchedId: String) {
        viewModelScope.launch {
            _watchState.value.animeDetailComplement?.let { complement ->
                if (complement.lastEpisodeWatchedId == lastEpisodeWatchedId) return@launch
                val updatedComplement = complement.copy(lastEpisodeWatchedId = lastEpisodeWatchedId)
                animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(updatedComplement)
            }
        }
    }

    private fun updateStoredWatchState(
        currentPosition: Long?,
        duration: Long?,
        screenShot: String?
    ) {
        _watchState.value.episodeDetailComplement?.let { episodeDetailComplement ->
            viewModelScope.launch {
                val cachedEpisodeDetailComplement =
                    animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(
                        episodeDetailComplement.id
                    )
                cachedEpisodeDetailComplement?.let {
                    animeEpisodeDetailRepository.updateEpisodeDetailComplement(
                        it.copy(
                            lastTimestamp = currentPosition,
                            duration = duration,
                            screenshot = screenShot,
                            lastWatched = LocalDateTime.now()
                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                    )
                    return@launch
                }

                animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(
                    episodeDetailComplement.copy(
                        lastTimestamp = currentPosition,
                        duration = duration,
                        screenshot = screenShot,
                        lastWatched = LocalDateTime.now()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    )
                )
            }
        }
    }

    private fun loadEpisodeDetailComplement(episodeId: String) = viewModelScope.launch {
        _watchState.update {
            it.copy(
                episodeDetailComplements = it.episodeDetailComplements + (episodeId to Resource.Loading())
            )
        }
        val cachedComplement = getCachedEpisodeDetailComplement(episodeId)
        if (cachedComplement != null) {
            _watchState.update {
                it.copy(
                    episodeDetailComplements = it.episodeDetailComplements + (episodeId to Resource.Success(
                        cachedComplement
                    ))
                )
            }
            return@launch
        }

        _watchState.update {
            it.copy(
                episodeDetailComplements = it.episodeDetailComplements + (episodeId to Resource.Error(
                    "Episode detail complement not found"
                ))
            )
        }
    }

    private fun updateFavoriteInComplement(isFavorite: Boolean) {
        viewModelScope.launch {
            _watchState.value.episodeDetailComplement?.let { complement ->
                ComplementUtils.toggleEpisodeFavorite(
                    repository = animeEpisodeDetailRepository,
                    episodeId = complement.id,
                    isFavorite = isFavorite
                )
            }
        }
    }

    private suspend fun getCachedEpisodeDetailComplement(episodeId: String): EpisodeDetailComplement? {
        return animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId)
    }

    private fun restoreDefaultValues() {
        _defaultEpisodeDetailComplement.value?.let { default ->
            _watchState.update {
                it.copy(
                    episodeDetailComplement = default,
                    episodeSourcesQuery = default.sourcesQuery,
                    episodeDetailComplements = it.episodeDetailComplements + (default.id to Resource.Success(
                        default
                    ))
                )
            }
        }
    }
}
