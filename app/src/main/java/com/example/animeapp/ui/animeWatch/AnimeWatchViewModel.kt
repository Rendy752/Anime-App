package com.example.animeapp.ui.animeWatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.models.episodeSourcesQueryPlaceholder
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.StreamingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimeWatchViewModel @Inject constructor(
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
) : ViewModel() {
    private val _animeDetail = MutableStateFlow<AnimeDetail?>(null)
    val animeDetail: StateFlow<AnimeDetail?> = _animeDetail.asStateFlow()

    private val _episodes = MutableStateFlow<List<Episode>?>(null)
    val episodes: StateFlow<List<Episode>?> = _episodes.asStateFlow()

    private val _animeDetailComplement = MutableStateFlow<AnimeDetailComplement?>(null)
    private val _defaultEpisodeDetailComplement = MutableStateFlow<EpisodeDetailComplement?>(null)

    private val _episodeDetailComplement =
        MutableStateFlow<Resource<EpisodeDetailComplement>>(Resource.Loading())
    val episodeDetailComplement: StateFlow<Resource<EpisodeDetailComplement>> =
        _episodeDetailComplement.asStateFlow()

    private val _episodeSourcesQuery = MutableStateFlow<EpisodeSourcesQuery>(
        episodeSourcesQueryPlaceholder
    )
    val episodeSourcesQuery: StateFlow<EpisodeSourcesQuery> = _episodeSourcesQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun setInitialState(
        animeDetail: AnimeDetail,
        animeDetailComplement: AnimeDetailComplement,
        episodes: List<Episode>,
        defaultEpisode: EpisodeDetailComplement?,
    ) {
        _animeDetail.value = animeDetail
        _animeDetailComplement.value = animeDetailComplement
        _episodes.value = episodes
        _defaultEpisodeDetailComplement.value = defaultEpisode
        restoreDefaultValues()
    }

    fun handleSelectedEpisodeServer(
        episodeSourcesQuery: EpisodeSourcesQuery,
        isFirstInit: Boolean = false,
        isRefreshed: Boolean = false
    ) = viewModelScope.launch {
        try {
            _isRefreshing.value = true
            _episodeDetailComplement.value = Resource.Loading()
            if (!isRefreshed) {
                val cachedEpisodeDetailComplement =
                    getCachedEpisodeDetailComplement(episodeSourcesQuery.id)

                if (cachedEpisodeDetailComplement != null) {
                    if (cachedEpisodeDetailComplement.sourcesQuery == episodeSourcesQuery || isFirstInit) {
                        _episodeSourcesQuery.value = cachedEpisodeDetailComplement.sourcesQuery
                        _episodeDetailComplement.value =
                            Resource.Success(cachedEpisodeDetailComplement)
                        return@launch
                    } else {
                        val episodeServersResource =
                            Resource.Success(cachedEpisodeDetailComplement.servers)

                        val episodeSourcesResource = StreamingUtils.getEpisodeSources(
                            episodeServersResource,
                            { id, server, category ->
                                animeEpisodeDetailRepository.getEpisodeSources(id, server, category)
                            },
                            episodeSourcesQuery
                        )

                        if (episodeSourcesResource !is Resource.Success) {
                            restoreDefaultValues()
                            _episodeDetailComplement.value =
                                Resource.Error(
                                    episodeSourcesResource.message
                                        ?: "Failed to fetch episode sources"
                                )
                            return@launch
                        }

                        episodeServersResource.data.let { servers ->
                            episodeSourcesResource.data.let { sources ->
                                val updatedEpisodeDetailComplement =
                                    cachedEpisodeDetailComplement.copy(
                                        servers = servers,
                                        sources = sources,
                                        sourcesQuery = episodeSourcesQuery
                                    )

                                updateEpisodeDetailComplement(updatedEpisodeDetailComplement)
                                _episodeSourcesQuery.value = episodeSourcesQuery
                                _episodeDetailComplement.value =
                                    Resource.Success(updatedEpisodeDetailComplement)
                                return@launch
                            }
                        }
                    }
                }
            }

            val episodeServersResource =
                animeEpisodeDetailRepository.getEpisodeServers(episodeSourcesQuery.id)
            if (episodeServersResource !is Resource.Success) {
                restoreDefaultValues()
                _episodeDetailComplement.value =
                    Resource.Error(
                        episodeServersResource.message ?: "Failed to fetch episode servers"
                    )
                return@launch
            }

            val episodeSourcesResource = StreamingUtils.getEpisodeSources(
                episodeServersResource,
                { id, server, category ->
                    animeEpisodeDetailRepository.getEpisodeSources(id, server, category)
                },
                episodeSourcesQuery
            )

            if (episodeSourcesResource !is Resource.Success) {
                restoreDefaultValues()
                _episodeDetailComplement.value =
                    Resource.Error(
                        episodeSourcesResource.message ?: "Failed to fetch episode sources"
                    )
                return@launch
            }

            episodeServersResource.data.let { servers ->
                episodeSourcesResource.data.let { sources ->
                    var cachedEpisodeDetailComplement =
                        getCachedEpisodeDetailComplement(episodeSourcesQuery.id)
                    if (cachedEpisodeDetailComplement != null) {
                        cachedEpisodeDetailComplement = cachedEpisodeDetailComplement.copy(
                            servers = servers,
                            sources = sources,
                            sourcesQuery = episodeSourcesQuery
                        )
                        updateEpisodeDetailComplement(cachedEpisodeDetailComplement)
                        _episodeDetailComplement.value =
                            Resource.Success(cachedEpisodeDetailComplement)
                    } else {
                        animeDetail.value?.let { animeDetail ->
                            val currentEpisode =
                                _episodes.value?.firstOrNull { it.episodeId == servers.episodeId }
                            currentEpisode?.let { currentEpisode ->
                                val remoteEpisodeDetailComplement = EpisodeDetailComplement(
                                    id = currentEpisode.episodeId,
                                    animeTitle = animeDetail.title,
                                    episodeTitle = currentEpisode.name,
                                    imageUrl = animeDetail.images.jpg.image_url,
                                    number = currentEpisode.episodeNo,
                                    isFiller = currentEpisode.filler,
                                    servers = servers,
                                    sources = sources,
                                    sourcesQuery = episodeSourcesQuery
                                )

                                animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(
                                    remoteEpisodeDetailComplement
                                )

                                _episodeDetailComplement.value =
                                    Resource.Success(remoteEpisodeDetailComplement)
                            }
                        }
                    }
                    _episodeSourcesQuery.value = episodeSourcesQuery
                }
            }
        } catch (e: Exception) {
            restoreDefaultValues()
            _episodeDetailComplement.value =
                Resource.Error(e.message ?: "An unexpected error occurred")
        } finally {
            _isRefreshing.value = false
        }
    }

    fun updateLastEpisodeWatchedIdAnimeDetailComplement(lastEpisodeWatchedId: String) =
        viewModelScope.launch {
            _animeDetailComplement.value?.let {
                if (it.lastEpisodeWatchedId == lastEpisodeWatchedId) return@launch
                animeEpisodeDetailRepository.updateAnimeDetailComplement(
                    it.copy(lastEpisodeWatchedId = lastEpisodeWatchedId)
                )
                _animeDetailComplement.value = it.copy(lastEpisodeWatchedId = lastEpisodeWatchedId)
            }
        }

    suspend fun getCachedEpisodeDetailComplement(episodeId: String): EpisodeDetailComplement? =
        animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId)

    fun updateEpisodeDetailComplement(
        updatedEpisodeDetailComplement: EpisodeDetailComplement,
    ) =
        viewModelScope.launch {
            animeEpisodeDetailRepository.updateEpisodeDetailComplement(
                updatedEpisodeDetailComplement
            )
        }

    fun restoreDefaultValues() {
        _defaultEpisodeDetailComplement.value?.let { default ->
            _episodeDetailComplement.value = Resource.Success(default)
            _episodeSourcesQuery.value = default.sourcesQuery
        }
    }
}