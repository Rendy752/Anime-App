package com.example.animeapp.ui.episodeHistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeHistoryQueryState
import com.example.animeapp.models.Items
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.utils.ComplementUtils
import com.example.animeapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

data class EpisodeHistoryState(
    val episodeHistoryResults: Resource<Map<AnimeDetailComplement, List<EpisodeDetailComplement>>> = Resource.Loading(),
    val queryState: EpisodeHistoryQueryState = EpisodeHistoryQueryState(),
    val pagination: CompletePagination? = null,
    val isRefreshing: Boolean = false
)

sealed class EpisodeHistoryAction {
    object FetchHistory : EpisodeHistoryAction()
    data class ApplyFilters(val updatedQueryState: EpisodeHistoryQueryState) : EpisodeHistoryAction()
    data class ToggleEpisodeFavorite(val episodeId: String, val isFavorite: Boolean) : EpisodeHistoryAction()
    data class ToggleAnimeFavorite(val malId: Int, val isFavorite: Boolean) : EpisodeHistoryAction()
    data class DeleteEpisode(val episodeId: String) : EpisodeHistoryAction()
    data class DeleteAnime(val malId: Int) : EpisodeHistoryAction()
}

@HiltViewModel
class EpisodeHistoryViewModel @Inject constructor(
    private val repository: AnimeEpisodeDetailRepository
) : ViewModel() {

    private val _historyState = MutableStateFlow(EpisodeHistoryState())
    val historyState: StateFlow<EpisodeHistoryState> = _historyState.asStateFlow()

    init {
        onAction(EpisodeHistoryAction.FetchHistory)
    }

    fun onAction(action: EpisodeHistoryAction) {
        when (action) {
            EpisodeHistoryAction.FetchHistory -> fetchHistory()
            is EpisodeHistoryAction.ApplyFilters -> {
                _historyState.update { it.copy(queryState = action.updatedQueryState) }
                fetchHistory()
            }
            is EpisodeHistoryAction.ToggleEpisodeFavorite -> toggleEpisodeFavorite(
                action.episodeId,
                action.isFavorite
            )
            is EpisodeHistoryAction.ToggleAnimeFavorite -> toggleAnimeFavorite(
                action.malId,
                action.isFavorite
            )
            is EpisodeHistoryAction.DeleteEpisode -> deleteEpisode(action.episodeId)
            is EpisodeHistoryAction.DeleteAnime -> deleteAnime(action.malId)
        }
    }

    private fun fetchHistory() {
        viewModelScope.launch {
            val queryState = _historyState.value.queryState
            _historyState.update {
                it.copy(
                    isRefreshing = true,
                    episodeHistoryResults = Resource.Loading()
                )
            }

            val episodesResult = repository.getPaginatedEpisodeHistory(queryState)
            val totalCountResult = repository.getEpisodeHistoryCount(
                queryState.searchQuery,
                queryState.isFavorite
            )

            _historyState.update {
                if (episodesResult is Resource.Success && totalCountResult is Resource.Success) {
                    val animeEpisodeMap = episodesResult.data.groupBy { it.malId }
                        .mapNotNull { (malId, episodes) ->
                            ComplementUtils.getOrCreateAnimeDetailComplement(
                                repository = repository,
                                malId = malId
                            )?.let { it to episodes }
                        }.toMap()

                    val totalCount = totalCountResult.data
                    val lastVisiblePage = ceil(totalCount.toDouble() / queryState.limit).toInt()
                    val pagination = CompletePagination(
                        last_visible_page = lastVisiblePage,
                        has_next_page = queryState.page < lastVisiblePage,
                        current_page = queryState.page,
                        items = Items(
                            count = episodesResult.data.size,
                            total = totalCount,
                            per_page = queryState.limit
                        )
                    )

                    it.copy(
                        isRefreshing = false,
                        episodeHistoryResults = Resource.Success(animeEpisodeMap),
                        pagination = pagination
                    )
                } else {
                    val errorMessage = (episodesResult as? Resource.Error)?.message
                        ?: (totalCountResult as? Resource.Error)?.message
                        ?: "Failed to fetch episode history"
                    it.copy(
                        isRefreshing = false,
                        episodeHistoryResults = Resource.Error(errorMessage),
                        pagination = null
                    )
                }
            }
        }
    }

    private fun toggleEpisodeFavorite(episodeId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            val episode = repository.getCachedEpisodeDetailComplement(episodeId)
            if (episode == null) {
                _historyState.update {
                    it.copy(episodeHistoryResults = Resource.Error("Episode not found"))
                }
                return@launch
            }
            val updatedEpisode = episode.copy(isFavorite = isFavorite)
            repository.updateEpisodeDetailComplement(updatedEpisode)
            updateEpisodeInState(updatedEpisode)
        }
    }

    private fun toggleAnimeFavorite(malId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            val anime = repository.getCachedAnimeDetailComplementByMalId(malId)
            if (anime == null) {
                _historyState.update {
                    it.copy(episodeHistoryResults = Resource.Error("Anime not found"))
                }
                return@launch
            }
            val updatedAnime = anime.copy(isFavorite = isFavorite)
            repository.updateCachedAnimeDetailComplement(updatedAnime)
            updateAnimeInState(updatedAnime)
        }
    }

    private fun deleteEpisode(episodeId: String) {
        viewModelScope.launch {
            val success = repository.deleteEpisodeDetailComplement(episodeId)
            if (!success) {
                _historyState.update {
                    it.copy(episodeHistoryResults = Resource.Error("Episode not found"))
                }
                return@launch
            }
            removeEpisodeFromState(episodeId)
        }
    }

    private fun deleteAnime(malId: Int) {
        viewModelScope.launch {
            val success = repository.deleteAnimeDetailComplement(malId)
            if (!success) {
                _historyState.update {
                    it.copy(episodeHistoryResults = Resource.Error("Anime not found"))
                }
                return@launch
            }
            removeAnimeFromState(malId)
        }
    }

    private fun updateEpisodeInState(updatedEpisode: EpisodeDetailComplement) {
        _historyState.update { state ->
            val currentResults = state.episodeHistoryResults
            if (currentResults !is Resource.Success) return@update state
            val updatedMap = currentResults.data.mapValues { (_, episodes) ->
                episodes.map { episode ->
                    if (episode.id == updatedEpisode.id) updatedEpisode else episode
                }
            }
            state.copy(episodeHistoryResults = Resource.Success(updatedMap))
        }
    }

    private fun updateAnimeInState(updatedAnime: AnimeDetailComplement) {
        _historyState.update { state ->
            val currentResults = state.episodeHistoryResults
            if (currentResults !is Resource.Success) return@update state
            val updatedMap = currentResults.data.mapKeys { (anime, _) ->
                if (anime.malId == updatedAnime.malId) updatedAnime else anime
            }
            state.copy(episodeHistoryResults = Resource.Success(updatedMap))
        }
    }

    private fun removeEpisodeFromState(episodeId: String) {
        _historyState.update { state ->
            val currentResults = state.episodeHistoryResults
            if (currentResults !is Resource.Success) return@update state
            val updatedMap = currentResults.data.mapValues { (_, episodes) ->
                episodes.filter { it.id != episodeId }
            }.filterValues { it.isNotEmpty() }
            state.copy(episodeHistoryResults = Resource.Success(updatedMap))
        }
    }

    private fun removeAnimeFromState(malId: Int) {
        _historyState.update { state ->
            val currentResults = state.episodeHistoryResults
            if (currentResults !is Resource.Success) return@update state
            val updatedMap = currentResults.data.filterKeys { it.malId != malId }
            state.copy(episodeHistoryResults = Resource.Success(updatedMap))
        }
    }
}