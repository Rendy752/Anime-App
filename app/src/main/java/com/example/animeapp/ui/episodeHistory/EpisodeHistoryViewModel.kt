package com.example.animeapp.ui.episodeHistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeHistoryQueryState
import com.example.animeapp.models.Items
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

data class EpisodeHistoryState(
    val episodeHistoryResults: Resource<List<EpisodeDetailComplement>> = Resource.Loading(),
    val queryState: EpisodeHistoryQueryState = EpisodeHistoryQueryState(),
    val pagination: CompletePagination? = null,
    val isRefreshing: Boolean = false
)

sealed class EpisodeHistoryAction {
    object FetchHistory : EpisodeHistoryAction()
    data class ApplyFilters(val updatedQueryState: EpisodeHistoryQueryState) :
        EpisodeHistoryAction()

    data class ToggleFavorite(val episodeId: String, val isFavorite: Boolean) :
        EpisodeHistoryAction()
}

@HiltViewModel
class EpisodeHistoryViewModel @Inject constructor(
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
) : ViewModel() {

    private val _historyState = MutableStateFlow(EpisodeHistoryState())
    val historyState: StateFlow<EpisodeHistoryState> = _historyState.asStateFlow()

    init {
        onAction(EpisodeHistoryAction.FetchHistory)
    }

    fun onAction(action: EpisodeHistoryAction) {
        when (action) {
            EpisodeHistoryAction.FetchHistory -> fetchHistory()
            is EpisodeHistoryAction.ApplyFilters -> applyFilters(action.updatedQueryState)
            is EpisodeHistoryAction.ToggleFavorite -> toggleFavorite(
                action.episodeId,
                action.isFavorite
            )
        }
    }

    private fun fetchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val queryState = _historyState.value.queryState
            _historyState.update {
                it.copy(
                    isRefreshing = true,
                    episodeHistoryResults = Resource.Loading()
                )
            }

            val offset = (queryState.page - 1) * queryState.limit
            val episodesResult = animeEpisodeDetailRepository.getPaginatedEpisodeHistory(
                searchQuery = queryState.searchQuery,
                isFavorite = queryState.isFavorite,
                sortBy = queryState.sortBy.name,
                limit = queryState.limit,
                offset = offset
            )

            val totalCountResult = animeEpisodeDetailRepository.getEpisodeHistoryCount(
                searchQuery = queryState.searchQuery,
                isFavorite = queryState.isFavorite
            )

            when {
                episodesResult is Resource.Success && totalCountResult is Resource.Success -> {
                    val episodes = episodesResult.data
                    val totalCount = totalCountResult.data
                    val lastVisiblePage = ceil(totalCount.toDouble() / queryState.limit).toInt()
                    val hasNextPage = queryState.page < lastVisiblePage
                    val pagination = CompletePagination(
                        last_visible_page = lastVisiblePage,
                        has_next_page = hasNextPage,
                        current_page = queryState.page,
                        items = Items(
                            count = episodes.size,
                            total = totalCount,
                            per_page = queryState.limit
                        )
                    )

                    _historyState.update {
                        it.copy(
                            isRefreshing = false,
                            episodeHistoryResults = Resource.Success(episodes),
                            pagination = pagination
                        )
                    }
                }

                else -> {
                    val errorMessage = when {
                        episodesResult is Resource.Error -> episodesResult.message
                        totalCountResult is Resource.Error -> totalCountResult.message
                        else -> "Unknown error fetching episode history"
                    }
                    _historyState.update {
                        it.copy(
                            isRefreshing = false,
                            episodeHistoryResults = Resource.Error(
                                errorMessage ?: "Failed to fetch episode history"
                            ),
                            pagination = null
                        )
                    }
                }
            }
        }
    }

    private fun applyFilters(updatedQueryState: EpisodeHistoryQueryState) {
        _historyState.update { it.copy(queryState = updatedQueryState) }
        onAction(EpisodeHistoryAction.FetchHistory)
    }

    private fun toggleFavorite(episodeId: String, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val episode =
                    animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId)
                if (episode != null) {
                    val updatedEpisode = episode.copy(isFavorite = isFavorite)
                    animeEpisodeDetailRepository.updateEpisodeDetailComplement(updatedEpisode)
                    fetchHistory() // Refresh list to reflect favorite change
                } else {
                    _historyState.update {
                        it.copy(
                            episodeHistoryResults = Resource.Error("Episode not found"),
                            isRefreshing = false
                        )
                    }
                }
            } catch (e: Exception) {
                _historyState.update {
                    it.copy(
                        episodeHistoryResults = Resource.Error("Failed to update favorite: ${e.message}"),
                        isRefreshing = false
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}