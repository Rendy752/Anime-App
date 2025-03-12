package com.example.animeapp.ui.animeWatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.repository.AnimeStreamingRepository
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import com.example.animeapp.utils.StreamingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimeWatchViewModel @Inject constructor(
    private val animeStreamingRepository: AnimeStreamingRepository
) : ViewModel() {
    private val _animeDetail = MutableStateFlow<AnimeDetail?>(null)
    val animeDetail: StateFlow<AnimeDetail?> = _animeDetail.asStateFlow()

    private val _episodes = MutableStateFlow<List<Episode>?>(null)
    val episodes: StateFlow<List<Episode>?> = _episodes.asStateFlow()

    private val _defaultEpisodeDetailComplement = MutableStateFlow<EpisodeDetailComplement?>(null)

    private val _episodeDetailComplement =
        MutableStateFlow<Resource<EpisodeDetailComplement>>(Resource.Loading())
    val episodeDetailComplement: StateFlow<Resource<EpisodeDetailComplement>> =
        _episodeDetailComplement.asStateFlow()

    private val _episodeSourcesQuery = MutableStateFlow<EpisodeSourcesQuery?>(null)
    val episodeSourcesQuery: StateFlow<EpisodeSourcesQuery?> = _episodeSourcesQuery.asStateFlow()

    fun setInitialState(
        animeDetail: AnimeDetail,
        episodes: List<Episode>,
        defaultEpisode: EpisodeDetailComplement?,
    ) {
        _animeDetail.value = animeDetail
        _episodes.value = episodes
        _defaultEpisodeDetailComplement.value = defaultEpisode
        restoreDefaultValues()
    }

    fun handleSelectedEpisodeServer(
        episodeId: String,
        episodeSourcesQuery: EpisodeSourcesQuery? = null,
        isRefreshed: Boolean = false
    ) = viewModelScope.launch {
        _episodeDetailComplement.value = Resource.Loading()

        if (episodeId == _defaultEpisodeDetailComplement.value?.id &&
            episodeSourcesQuery == _episodeSourcesQuery.value &&
            !isRefreshed
        ) return@launch

        if (!isRefreshed) {
            val cachedEpisodeDetailComplement =
                animeStreamingRepository.getCachedEpisodeDetailComplement(episodeId)

            if (cachedEpisodeDetailComplement != null) {
                _episodeDetailComplement.value = Resource.Success(cachedEpisodeDetailComplement)
                return@launch
            }
        }

        val episodeServersResponse = animeStreamingRepository.getEpisodeServers(episodeId)
        val episodeServersResource = ResponseHandler.handleCommonResponse(episodeServersResponse)

        if (episodeServersResource !is Resource.Success) {
            restoreDefaultValues()
            _episodeDetailComplement.value =
                Resource.Error(episodeServersResource.message ?: "Failed to fetch episode servers")
            return@launch
        }

        val episodeSourcesResource = StreamingUtils.getEpisodeSources(
            episodeServersResource,
            animeStreamingRepository,
            episodeSourcesQuery
        )

        if (episodeSourcesResource !is Resource.Success) {
            restoreDefaultValues()
            _episodeDetailComplement.value =
                Resource.Error(episodeSourcesResource.message ?: "Failed to fetch episode sources")
            return@launch
        }

        _episodeSourcesQuery.value =
            episodeSourcesQuery ?: StreamingUtils.getEpisodeQuery(episodeServersResource, episodeId)

        episodeServersResource.data?.let { servers ->
            episodeSourcesResource.data?.let { sources ->
                val remoteEpisodeDetailComplement = animeDetail.value?.let { animeDetail ->
                    EpisodeDetailComplement(
                        id = servers.episodeId,
                        title = animeDetail.title,
                        imageUrl = animeDetail.images.jpg.image_url,
                        servers = servers,
                        sources = sources
                    )
                }

                if (remoteEpisodeDetailComplement != null) {
                    val cachedEpisodeDetailComplement =
                        animeStreamingRepository.getCachedEpisodeDetailComplement(episodeId)

                    if (cachedEpisodeDetailComplement == null || cachedEpisodeDetailComplement.servers != remoteEpisodeDetailComplement.servers || cachedEpisodeDetailComplement.sources != remoteEpisodeDetailComplement.sources) {
                        animeStreamingRepository.insertCachedEpisodeDetailComplement(
                            remoteEpisodeDetailComplement
                        )
                    }
                    _episodeDetailComplement.value = Resource.Success(remoteEpisodeDetailComplement)
                }
            }
        }
    }

    private fun restoreDefaultValues() {
        _defaultEpisodeDetailComplement.value?.let { default ->
            _episodeDetailComplement.value = Resource.Success(default)
            _episodeSourcesQuery.value = StreamingUtils.getEpisodeQuery(
                Resource.Success(default.servers),
                default.id
            )
        }
    }
}