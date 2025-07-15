package com.luminoverse.animevibe.ui.episodeHistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminoverse.animevibe.models.AnimeDetailComplement
import com.luminoverse.animevibe.models.CompletePagination
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeHistoryQueryState
import com.luminoverse.animevibe.models.defaultCompletePagination
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.utils.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EpisodeHistoryState(
    val paginatedHistory: Resource<Map<AnimeDetailComplement, List<EpisodeDetailComplement>>> = Resource.Loading(),
    val isEpisodeHistoryEmpty: Boolean = true,
    val queryState: EpisodeHistoryQueryState = EpisodeHistoryQueryState(),
    val pagination: CompletePagination = defaultCompletePagination,
    val isRefreshing: Boolean = false
)

sealed class EpisodeHistoryAction {
    object FetchHistory : EpisodeHistoryAction()
    object CheckIfHistoryIsEmpty : EpisodeHistoryAction()
    data class ApplyFilters(val updatedQueryState: EpisodeHistoryQueryState) :
        EpisodeHistoryAction()

    data class ChangePage(val page: Int) : EpisodeHistoryAction()
    data class ToggleEpisodeFavorite(val episodeId: String, val isFavorite: Boolean) :
        EpisodeHistoryAction()

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

    fun onAction(action: EpisodeHistoryAction) {
        when (action) {
            is EpisodeHistoryAction.FetchHistory -> fetchHistory()
            is EpisodeHistoryAction.CheckIfHistoryIsEmpty -> checkIfHistoryIsEmpty()
            is EpisodeHistoryAction.ApplyFilters -> applyFilters(action.updatedQueryState)
            is EpisodeHistoryAction.ChangePage -> changePage(action.page)
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

    private fun checkIfHistoryIsEmpty() {
        viewModelScope.launch {
            val result = repository.getAllEpisodeHistory(EpisodeHistoryQueryState())
            _historyState.update {
                it.copy(isEpisodeHistoryEmpty = result.data.isNullOrEmpty())
            }
        }
    }

    private fun fetchHistory(isRefreshing: Boolean = true) {
        viewModelScope.launch {
            if (isRefreshing) {
                _historyState.update {
                    it.copy(
                        isRefreshing = true,
                        paginatedHistory = Resource.Loading()
                    )
                }
            }

            when (val result =
                repository.getPaginatedAndFilteredHistory(_historyState.value.queryState)) {
                is Resource.Success -> {
                    _historyState.update {
                        it.copy(
                            isRefreshing = false,
                            paginatedHistory = Resource.Success(result.data.data),
                            pagination = result.data.pagination,
                            queryState = it.queryState.copy(page = result.data.pagination.current_page)
                        )
                    }
                }

                is Resource.Error -> {
                    _historyState.update {
                        it.copy(
                            isRefreshing = false,
                            paginatedHistory = Resource.Error(result.message)
                        )
                    }
                }

                is Resource.Loading -> {}
            }
        }
    }

    private fun applyFilters(updatedQueryState: EpisodeHistoryQueryState) {
        _historyState.update { it.copy(queryState = updatedQueryState.copy(page = 1)) }
        if (_historyState.value.isEpisodeHistoryEmpty) return
        fetchHistory(isRefreshing = false)
    }

    private fun changePage(page: Int) {
        _historyState.update { it.copy(queryState = it.queryState.copy(page = page)) }
        fetchHistory(isRefreshing = false)
    }

    private fun toggleEpisodeFavorite(episodeId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            when (repository.toggleEpisodeFavorite(episodeId, isFavorite)) {
                is Resource.Success -> fetchHistory(isRefreshing = false)
                is Resource.Error -> {}
                else -> {}
            }
        }
    }

    private fun toggleAnimeFavorite(malId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            val anime = repository.getCachedAnimeDetailComplementByMalId(malId)
            repository.toggleAnimeFavorite(anime?.id, malId, isFavorite)
            fetchHistory(isRefreshing = false)
        }
    }

    private fun deleteEpisode(episodeId: String) {
        viewModelScope.launch {
            if (repository.deleteEpisodeDetailComplement(episodeId)) {
                fetchHistory(isRefreshing = false)
            }
        }
    }

    private fun deleteAnime(malId: Int) {
        viewModelScope.launch {
            repository.deleteAnimeDetailById(malId)
            if (repository.deleteAnimeDetailComplement(malId)) {
                fetchHistory(isRefreshing = false)
            }
        }
    }
}