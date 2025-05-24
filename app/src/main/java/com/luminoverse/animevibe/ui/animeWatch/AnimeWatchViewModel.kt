package com.luminoverse.animevibe.ui.animeWatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.AnimeDetailComplement
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.episodeSourcesQueryPlaceholder
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.utils.ComplementUtils
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.media.StreamingUtils
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
    val episodeSourcesQuery: EpisodeSourcesQuery = episodeSourcesQueryPlaceholder,
    val isRefreshing: Boolean = false,
    val isFavorite: Boolean = false,
    val errorMessage: String? = null,
    val selectedContentIndex: Int = 0
)

data class PlayerUiState(
    val isFullscreen: Boolean = false,
    val isPipMode: Boolean = false,
    val isLocked: Boolean = false,
    val isShowResumeOverlay: Boolean = false,
    val isShowNextEpisode: Boolean = false,
    val nextEpisodeName: String = "",
    val isShowPip: Boolean = false,
    val isShowSpeedUp: Boolean = false,
    val speedUpText: String = "1x speed",
    val isShowSeekIndicator: Boolean = false,
    val seekDirection: Int = 0,
    val seekAmount: Long = 0L,
    val isSeeking: Boolean = false
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
    data class SetFullscreen(val isFullscreen: Boolean) : WatchAction()
    data class SetPipMode(val isPipMode: Boolean) : WatchAction()
    data class SetLocked(val isLocked: Boolean) : WatchAction()
    data class SetShowResumeOverlay(val isShow: Boolean) : WatchAction()
    data class SetShowNextEpisode(val isShow: Boolean, val nextEpisodeName: String = "") :
        WatchAction()

    data class SetSelectedContentIndex(val index: Int) : WatchAction()
    data class SetErrorMessage(val message: String?) : WatchAction()
    data class SetFavorite(val isFavorite: Boolean, val updateComplement: Boolean = true) :
        WatchAction()
}

@HiltViewModel
class AnimeWatchViewModel @Inject constructor(
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
) : ViewModel() {

    private val _watchState = MutableStateFlow(WatchState())
    val watchState: StateFlow<WatchState> = _watchState.asStateFlow()

    private val _playerUiState = MutableStateFlow(PlayerUiState())
    val playerUiState: StateFlow<PlayerUiState> = _playerUiState.asStateFlow()

    private val _defaultEpisodeDetailComplement = MutableStateFlow<EpisodeDetailComplement?>(null)

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
            is WatchAction.SetFullscreen -> _playerUiState.update { it.copy(isFullscreen = action.isFullscreen) }
            is WatchAction.SetPipMode -> _playerUiState.update { it.copy(isPipMode = action.isPipMode) }
            is WatchAction.SetLocked -> _playerUiState.update { it.copy(isLocked = action.isLocked) }
            is WatchAction.SetShowResumeOverlay -> _playerUiState.update {
                it.copy(
                    isShowResumeOverlay = action.isShow
                )
            }

            is WatchAction.SetShowNextEpisode -> _playerUiState.update {
                it.copy(isShowNextEpisode = action.isShow, nextEpisodeName = action.nextEpisodeName)
            }

            is WatchAction.SetSelectedContentIndex -> _watchState.update {
                it.copy(
                    selectedContentIndex = action.index
                )
            }

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
                    ?: episodeSourcesQueryPlaceholder.copy(id = episodeId)
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
                    episodeDetailComplement = Resource.Loading()
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
                                isFavorite = newIsFavorite
                            )
                        }
                        return@launch
                    } else {
                        val episodeServersResource =
                            Resource.Success(cachedEpisodeDetailComplement.servers)
                        val (episodeSourcesResource) = StreamingUtils.getEpisodeSources(
                            episodeServersResource,
                            { id, server, category ->
                                animeEpisodeDetailRepository.getEpisodeSources(
                                    id,
                                    server,
                                    category
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
                                    )
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
                                        sourcesQuery = episodeSourcesQuery
                                    )
                                updateEpisodeDetailComplement(updatedEpisodeDetailComplement)
                                _watchState.update {
                                    it.copy(
                                        episodeSourcesQuery = episodeSourcesQuery,
                                        episodeDetailComplement = Resource.Success(
                                            updatedEpisodeDetailComplement
                                        ),
                                        isFavorite = newIsFavorite
                                    )
                                }
                                return@launch
                            }
                        }
                    }
                }
            }

            val episodeServersResource =
                animeEpisodeDetailRepository.getEpisodeServers(episodeSourcesQuery.id)
            if (episodeServersResource !is Resource.Success) {
                restoreDefaultValues(tempEpisodeDetailComplement)
                _watchState.update {
                    it.copy(
                        episodeDetailComplement = Resource.Error(
                            episodeServersResource.message ?: "Failed to fetch episode servers"
                        )
                    )
                }
                return@launch
            }

            val (episodeSourcesResource) = StreamingUtils.getEpisodeSources(
                episodeServersResource,
                { id, server, category ->
                    animeEpisodeDetailRepository.getEpisodeSources(
                        id,
                        server,
                        category
                    )
                },
                episodeSourcesQuery
            )
            if (episodeSourcesResource !is Resource.Success) {
                restoreDefaultValues(tempEpisodeDetailComplement)
                _watchState.update {
                    it.copy(
                        episodeDetailComplement = Resource.Error(
                            episodeSourcesResource.message ?: "Failed to fetch episode sources"
                        )
                    )
                }
                return@launch
            }

            episodeServersResource.data.let { servers ->
                episodeSourcesResource.data.let { sources ->
                    var cachedEpisodeDetailComplement =
                        getCachedEpisodeDetailComplement(episodeSourcesQuery.id)
                    val newIsFavorite = cachedEpisodeDetailComplement?.isFavorite == true
                    if (cachedEpisodeDetailComplement != null) {
                        cachedEpisodeDetailComplement = cachedEpisodeDetailComplement.copy(
                            servers = servers,
                            sources = sources,
                            sourcesQuery = episodeSourcesQuery
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
                        _watchState.value.animeDetail?.let { animeDetail ->
                            _watchState.value.animeDetailComplement?.let { animeDetailComplement ->
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
                                        sourcesQuery = episodeSourcesQuery
                                    ).let { remoteEpisodeDetailComplement ->
                                        _watchState.update {
                                            it.copy(
                                                episodeDetailComplement = Resource.Success(
                                                    remoteEpisodeDetailComplement
                                                ),
                                                episodeDetailComplements = it.episodeDetailComplements + (remoteEpisodeDetailComplement.id to Resource.Success(
                                                    remoteEpisodeDetailComplement
                                                )),
                                                isFavorite = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    _watchState.update { it.copy(episodeSourcesQuery = episodeSourcesQuery) }
                }
            }
        } catch (e: Exception) {
            restoreDefaultValues(tempEpisodeDetailComplement)
            _watchState.update {
                it.copy(
                    episodeDetailComplement = Resource.Error(
                        e.message ?: "An unexpected error occurred"
                    )
                )
            }
        } finally {
            _watchState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun updateLastEpisodeWatchedId(lastEpisodeWatchedId: String) {
        viewModelScope.launch {
            _watchState.value.animeDetailComplement?.let { complement ->
                if (complement.lastEpisodeWatchedId == lastEpisodeWatchedId) return@launch
                val updatedComplement = complement.copy(lastEpisodeWatchedId = lastEpisodeWatchedId)
                animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(updatedComplement)
                _watchState.update { it.copy(animeDetailComplement = updatedComplement) }
            }
        }
    }

    private fun updateEpisodeDetailComplement(updatedEpisodeDetailComplement: EpisodeDetailComplement) {
        viewModelScope.launch {
            val updatedWithFavorite =
                updatedEpisodeDetailComplement.copy(isFavorite = _watchState.value.isFavorite)
            animeEpisodeDetailRepository.updateEpisodeDetailComplement(updatedWithFavorite)
            if (_watchState.value.episodeDetailComplement is Resource.Success) {
                _watchState.update {
                    it.copy(
                        episodeDetailComplement = Resource.Success(updatedWithFavorite),
                        episodeDetailComplements = it.episodeDetailComplements + (updatedWithFavorite.id to Resource.Success(
                            updatedWithFavorite
                        ))
                    )
                }
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