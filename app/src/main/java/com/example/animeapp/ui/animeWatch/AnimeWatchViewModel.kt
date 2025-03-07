package com.example.animeapp.ui.animeWatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.models.EpisodeWatch
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

    private val _defaultEpisodeServers = MutableStateFlow<EpisodeServersResponse?>(null)
    private val _defaultEpisodeSources = MutableStateFlow<EpisodeSourcesResponse?>(null)

    private val _episodeWatch = MutableStateFlow<Resource<EpisodeWatch>>(Resource.Loading())
    val episodeWatch: StateFlow<Resource<EpisodeWatch>> = _episodeWatch.asStateFlow()

    private val _episodeSourcesQuery = MutableStateFlow<EpisodeSourcesQuery?>(null)
    val episodeSourcesQuery: StateFlow<EpisodeSourcesQuery?> = _episodeSourcesQuery.asStateFlow()

    fun setInitialState(
        animeDetail: AnimeDetail,
        episodes: List<Episode>,
        defaultEpisodeServers: EpisodeServersResponse?,
        defaultEpisodeSources: EpisodeSourcesResponse?
    ) {
        _animeDetail.value = animeDetail
        _episodes.value = episodes
        _defaultEpisodeServers.value = defaultEpisodeServers
        _defaultEpisodeSources.value = defaultEpisodeSources
        restoreDefaultValues()
    }

    fun handleSelectedEpisodeServer(
        episodeId: String,
        episodeSourcesQuery: EpisodeSourcesQuery? = null
    ) = viewModelScope.launch {
        _episodeWatch.value = Resource.Loading()

        if (episodeId == _defaultEpisodeServers.value?.episodeId) {
            restoreDefaultValues()
            return@launch
        }

        if (episodeSourcesQuery == _episodeSourcesQuery.value) {
            restoreDefaultValues()
            return@launch
        }

        val episodeServersResponse = animeStreamingRepository.getEpisodeServers(episodeId)
        val episodeServerResource = ResponseHandler.handleCommonResponse(episodeServersResponse)

        if (episodeServerResource !is Resource.Success) {
            restoreDefaultValues()
            return@launch
        }

        val episodeSourcesResource = StreamingUtils.getEpisodeSources(
            episodeServerResource,
            animeStreamingRepository,
            episodeSourcesQuery
        )

        if (episodeSourcesResource !is Resource.Success) {
            restoreDefaultValues()
            return@launch
        }

        _episodeSourcesQuery.value =
            episodeSourcesQuery ?: StreamingUtils.getEpisodeQuery(episodeServerResource, episodeId)

        _episodeWatch.value = Resource.Success(
            EpisodeWatch(
                episodeServerResource.data!!,
                episodeSourcesResource.data!!
            )
        )
    }

    private fun restoreDefaultValues() {
        _episodeWatch.value = Resource.Success(
            EpisodeWatch(
                _defaultEpisodeServers.value!!,
                _defaultEpisodeSources.value!!
            )
        )
        _episodeSourcesQuery.value = StreamingUtils.getEpisodeQuery(
            Resource.Success(_defaultEpisodeServers.value!!),
            _defaultEpisodeServers.value!!.episodeId
        )
    }
}