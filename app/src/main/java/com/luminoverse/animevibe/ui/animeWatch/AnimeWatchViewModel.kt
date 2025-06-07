package com.luminoverse.animevibe.ui.animeWatch

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
import com.luminoverse.animevibe.utils.media.StreamingUtils.getDefaultEpisodeQueries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchState(
    val animeDetail: AnimeDetail? = null,
    val animeDetailComplement: AnimeDetailComplement? = null,
    val episodeDetailComplement: Resource<EpisodeDetailComplement> = Resource.Loading(),
    val episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>> = emptyMap(),
    val episodeSourcesQuery: EpisodeSourcesQuery? = null,
    val isRefreshing: Boolean = false,
    val isFavorite: Boolean = false,
    val errorMessage: String? = null,
    val newEpisodeCount: Int = 0
)

data class PlayerUiState(
    val isLoading: Boolean = false,
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
    data class UpdateEpisodeDetailComplement(val updatedEpisodeDetailComplement: EpisodeDetailComplement) :
        WatchAction()

    data class LoadEpisodeDetailComplement(val episodeId: String) : WatchAction()
    data class SetIsLoading(val isLoading: Boolean) : WatchAction()
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
                            it.copy(isShowNextEpisode = true, isLoading = false)
                        }
                        onAction(WatchAction.SetErrorMessage(null))
                    }

                    Player.STATE_READY -> {
                        _playerUiState.update { it.copy(isLoading = false) }
                        onAction(WatchAction.SetErrorMessage(null))
                    }

                    Player.STATE_BUFFERING -> {
                        _playerUiState.update { it.copy(isLoading = true) }
                        if (coreState.error == null) {
                            onAction(WatchAction.SetErrorMessage(null))
                        }
                    }

                    Player.STATE_IDLE -> {
                        _playerUiState.update { it.copy(isLoading = true) }
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
                action.episodeSourcesQuery,
                action.isFirstInit,
                action.isRefresh
            )

            is WatchAction.UpdateLastEpisodeWatchedId -> updateLastEpisodeWatchedId(action.lastEpisodeWatchedId)
            is WatchAction.UpdateEpisodeDetailComplement -> updateEpisodeDetailComplement(action.updatedEpisodeDetailComplement)
            is WatchAction.LoadEpisodeDetailComplement -> loadEpisodeDetailComplement(action.episodeId)

            is WatchAction.SetIsLoading -> _playerUiState.update { it.copy(isLoading = action.isLoading) }
            is WatchAction.SetFullscreen -> _playerUiState.update { it.copy(isFullscreen = action.isFullscreen) }
            is WatchAction.SetPipMode -> _playerUiState.update { it.copy(isPipMode = action.isPipMode) }
            is WatchAction.SetShowResume -> _playerUiState.update {
                it.copy(isShowResume = action.isShow)
            }

            is WatchAction.SetShowNextEpisode -> _playerUiState.update { it.copy(isShowNextEpisode = action.isShow) }
            is WatchAction.SetErrorMessage -> _watchState.update { it.copy(errorMessage = action.message) }

            is WatchAction.SetFavorite -> {
                _watchState.update { it.copy(isFavorite = action.isFavorite) }
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
                val initialIsFavorite = defaultEpisode?.isFavorite == true
                _watchState.update { it.copy(isFavorite = initialIsFavorite) }

                val initialQuery = defaultEpisode?.sourcesQuery?.copy(id = episodeId)
                    ?: EpisodeSourcesQuery(id = episodeId,
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
        val tempEpisodeDetailComplement =
            (_watchState.value.episodeDetailComplement as? Resource.Success)?.data
        try {
            _watchState.update {
                it.copy(
                    isRefreshing = true,
                    episodeSourcesQuery = episodeSourcesQuery,
                    episodeDetailComplement = Resource.Loading(null)
                )
            }
            if (!isRefresh) {
                val cachedEpisodeDetailComplement =
                    getCachedEpisodeDetailComplement(episodeSourcesQuery.id)
                if (cachedEpisodeDetailComplement != null) {
                    if (cachedEpisodeDetailComplement.sourcesQuery == episodeSourcesQuery || isFirstInit) {
                        val newIsFavorite = cachedEpisodeDetailComplement.isFavorite
                        _watchState.update {
                            it.copy(
                                episodeSourcesQuery = cachedEpisodeDetailComplement.sourcesQuery,
                                episodeDetailComplement = Resource.Success(
                                    cachedEpisodeDetailComplement
                                ),
                                isFavorite = newIsFavorite,
                                errorMessage = null
                            )
                        }
                        return@launch
                    } else {
                        val episodeServersResource =
                            Resource.Success(cachedEpisodeDetailComplement.servers)
                        val (episodeSourcesResource, availableEpisodeSourcesQuery) = StreamingUtils.getEpisodeSources(
                            episodeServersResource,
                            { id, server, category ->
                                animeEpisodeDetailRepository.getEpisodeSources(
                                    id, server, category
                                )
                            },
                            episodeSourcesQuery
                        )
                        if (episodeSourcesResource !is Resource.Success) {
                            restoreDefaultValues(tempEpisodeDetailComplement)
                            _watchState.update {
                                it.copy(
                                    episodeDetailComplement = Resource.Error(
                                        episodeSourcesResource.message
                                            ?: "Failed to fetch episode sources"
                                    ),
                                    errorMessage = "Failed to fetch episode sources, returning to the previous episode. Check your internet connection or try again later after 1 hour."
                                )
                            }
                            return@launch
                        }
                        episodeServersResource.data.let { servers ->
                            episodeSourcesResource.data.let { sources ->
                                val newIsFavorite = cachedEpisodeDetailComplement.isFavorite
                                val updatedEpisodeDetailComplement =
                                    cachedEpisodeDetailComplement.copy(
                                        servers = servers,
                                        sources = sources,
                                        sourcesQuery = availableEpisodeSourcesQuery
                                            ?: episodeSourcesQuery
                                    )
                                updateEpisodeDetailComplement(updatedEpisodeDetailComplement)
                                _watchState.update {
                                    it.copy(
                                        episodeSourcesQuery = availableEpisodeSourcesQuery
                                            ?: episodeSourcesQuery,
                                        episodeDetailComplement = Resource.Success(
                                            updatedEpisodeDetailComplement
                                        ),
                                        isFavorite = newIsFavorite,
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
                        restoreDefaultValues(tempEpisodeDetailComplement)
                        _watchState.update {
                            it.copy(
                                episodeDetailComplement = Resource.Error(
                                    episodeServersResource.message
                                        ?: "Failed to fetch episode servers"
                                ),
                                errorMessage = "Failed to fetch episode servers, returning to the previous episode. Check your internet connection or try again later after 1 hour."
                            )
                        }
                        return@launch
                    }

                    var currentQuery = episodeSourcesQuery
                    var attempt = 0
                    val defaultQueries =
                        getDefaultEpisodeQueries(episodeServersResource, episodeSourcesQuery.id)
                    val maxAttempts = defaultQueries.size

                    while (attempt < maxAttempts) {
                        val (episodeSourcesResource, newQuery) = StreamingUtils.getEpisodeSources(
                            episodeServersResource,
                            { id, server, category ->
                                animeEpisodeDetailRepository.getEpisodeSources(id, server, category)
                            },
                            currentQuery
                        )
                        if (episodeSourcesResource is Resource.Success) {
                            episodeServersResource.data.let { servers ->
                                episodeSourcesResource.data.let { sources ->
                                    var cachedEpisodeDetailComplement =
                                        getCachedEpisodeDetailComplement(episodeSourcesQuery.id)
                                    val newIsFavorite =
                                        cachedEpisodeDetailComplement?.isFavorite == true
                                    if (cachedEpisodeDetailComplement != null) {
                                        cachedEpisodeDetailComplement =
                                            cachedEpisodeDetailComplement.copy(
                                                servers = servers,
                                                sources = sources,
                                                sourcesQuery = newQuery ?: currentQuery
                                            )
                                        updateEpisodeDetailComplement(cachedEpisodeDetailComplement)
                                        _watchState.update {
                                            it.copy(
                                                episodeDetailComplement = Resource.Success(
                                                    cachedEpisodeDetailComplement
                                                ),
                                                isFavorite = newIsFavorite
                                            )
                                        }
                                    } else {
                                        val currentEpisode =
                                            animeDetailComplement.episodes?.firstOrNull { it.episodeId == servers.episodeId }
                                        currentEpisode?.let { episode ->
                                            ComplementUtils.createEpisodeDetailComplement(
                                                repository = animeEpisodeDetailRepository,
                                                animeDetail = animeDetail,
                                                animeDetailComplement = animeDetailComplement,
                                                episode = episode,
                                                servers = servers,
                                                sources = sources,
                                                sourcesQuery = newQuery ?: currentQuery
                                            ).let { remoteEpisodeDetailComplement ->
                                                _watchState.update {
                                                    it.copy(
                                                        episodeDetailComplement = Resource.Success(
                                                            remoteEpisodeDetailComplement
                                                        ),
                                                        episodeDetailComplements = it.episodeDetailComplements + (remoteEpisodeDetailComplement.id to Resource.Success(
                                                            remoteEpisodeDetailComplement
                                                        )),
                                                        isFavorite = false,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    _watchState.update {
                                        it.copy(
                                            episodeSourcesQuery = newQuery ?: currentQuery,
                                            errorMessage = null
                                        )
                                    }
                                    return@launch
                                }
                            }
                        } else {
                            attempt++
                            StreamingUtils.markServerFailed(
                                currentQuery.server,
                                currentQuery.category
                            )
                            if (newQuery != null) {
                                currentQuery = newQuery
                            } else {
                                val remainingQueries = defaultQueries
                                    .filter { "${it.server}-${it.category}" !in StreamingUtils.failedServers.keys }
                                if (remainingQueries.isNotEmpty()) {
                                    currentQuery = remainingQueries[0]
                                } else {
                                    restoreDefaultValues(tempEpisodeDetailComplement)
                                    _watchState.update {
                                        it.copy(
                                            episodeDetailComplement = Resource.Error(
                                                "Failed to fetch episode sources after $attempt attempts"
                                            ),
                                            errorMessage = "Failed to fetch episode sources after $attempt attempts, returning to the previous episode. Check your internet connection or try again later after 1 hour."
                                        )
                                    }
                                    return@launch
                                }
                            }
                        }
                    }
                    restoreDefaultValues(tempEpisodeDetailComplement)
                    _watchState.update {
                        it.copy(
                            episodeDetailComplement = Resource.Error(
                                "Failed to fetch episode sources after $maxAttempts attempts"
                            ),
                            errorMessage = "Failed to fetch episode sources after $maxAttempts attempts, returning to the previous episode. Check your internet connection or try again later after 1 hour."
                        )
                    }
                }
            }
        } catch (e: Exception) {
            restoreDefaultValues(tempEpisodeDetailComplement)
            _watchState.update {
                it.copy(
                    episodeDetailComplement = Resource.Error(
                        e.message ?: "An unexpected error occurred"
                    ),
                    errorMessage = e.message ?: "An unexpected error occurred"
                )
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

    private fun updateEpisodeDetailComplement(updatedEpisodeDetailComplement: EpisodeDetailComplement) {
        viewModelScope.launch {
            val updatedWithFavorite =
                updatedEpisodeDetailComplement.copy(isFavorite = _watchState.value.isFavorite)
            animeEpisodeDetailRepository.updateEpisodeDetailComplement(updatedWithFavorite)
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
            (_watchState.value.episodeDetailComplement as? Resource.Success)?.data?.let { complement ->
                ComplementUtils.toggleEpisodeFavorite(
                    repository = animeEpisodeDetailRepository,
                    episodeId = complement.id,
                    isFavorite = isFavorite
                )?.let { updatedComplement ->
                    _watchState.update {
                        it.copy(
                            episodeDetailComplement = Resource.Success(updatedComplement),
                            episodeDetailComplements = it.episodeDetailComplements + (updatedComplement.id to Resource.Success(
                                updatedComplement
                            ))
                        )
                    }
                }
            }
        }
    }

    private suspend fun getCachedEpisodeDetailComplement(episodeId: String): EpisodeDetailComplement? {
        return animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId)
    }

    private fun restoreDefaultValues(episodeDetailComplement: EpisodeDetailComplement?) {
        episodeDetailComplement?.let { complement ->
            _watchState.update {
                it.copy(
                    episodeDetailComplement = Resource.Success(complement),
                    episodeSourcesQuery = complement.sourcesQuery,
                    episodeDetailComplements = it.episodeDetailComplements + (complement.id to Resource.Success(
                        complement
                    ))
                )
            }
        } ?: run {
            _defaultEpisodeDetailComplement.value?.let { default ->
                _watchState.update {
                    it.copy(
                        episodeDetailComplement = Resource.Success(default),
                        episodeSourcesQuery = default.sourcesQuery,
                        episodeDetailComplements = it.episodeDetailComplements + (default.id to Resource.Success(
                            default
                        ))
                    )
                }
            }
        }
    }
}
