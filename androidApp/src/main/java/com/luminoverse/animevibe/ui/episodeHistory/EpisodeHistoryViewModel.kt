package com.luminoverse.animevibe.ui.episodeHistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminoverse.animevibe.models.AnimeDetailComplement
import com.luminoverse.animevibe.models.CompletePagination
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeHistoryQueryState
import com.luminoverse.animevibe.models.Items
import com.luminoverse.animevibe.models.defaultCompletePagination
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.utils.watch.AnimeTitleFinder
import com.luminoverse.animevibe.utils.ComplementUtils
import com.luminoverse.animevibe.utils.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

data class EpisodeHistoryState(
    val filteredEpisodeHistoryResults: Resource<Map<AnimeDetailComplement, List<EpisodeDetailComplement>>> = Resource.Loading(),
    val episodeHistoryResults: Resource<List<EpisodeDetailComplement>> = Resource.Loading(),
    val isEpisodeHistoryEmpty: Boolean = true,
    val queryState: EpisodeHistoryQueryState = EpisodeHistoryQueryState(),
    val pagination: CompletePagination = defaultCompletePagination,
    val isRefreshing: Boolean = false
)

sealed class EpisodeHistoryAction {
    object FetchHistory : EpisodeHistoryAction()
    object FetchAllHistory : EpisodeHistoryAction()
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

    private val episodeExtractors = listOf<(EpisodeDetailComplement) -> String>(
        { it.episodeTitle },
        { it.animeTitle }
    )

    fun onAction(action: EpisodeHistoryAction) {
        when (action) {
            EpisodeHistoryAction.FetchHistory -> fetchHistory()
            is EpisodeHistoryAction.FetchAllHistory -> fetchAllEpisodeHistory()
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

    private fun fetchAllEpisodeHistory() {
        viewModelScope.launch {
            val allEpisodeHistory = repository.getAllEpisodeHistory(EpisodeHistoryQueryState()).data
            _historyState.update {
                it.copy(
                    isEpisodeHistoryEmpty = allEpisodeHistory?.isEmpty() == true
                )
            }
        }
    }

    private fun fetchHistory(isRefreshing: Boolean = true) {
        viewModelScope.launch {
            val queryState = _historyState.value.queryState
            if (isRefreshing) {
                _historyState.update {
                    it.copy(
                        isRefreshing = true,
                        filteredEpisodeHistoryResults = Resource.Loading(),
                        episodeHistoryResults = Resource.Loading()
                    )
                }
            }

            val allEpisodesResult = repository.getAllEpisodeHistory(queryState)
            when (allEpisodesResult) {
                is Resource.Success -> {
                    val (filteredMap, pagination) = computeFilteredAndPaginatedData(
                        allEpisodesResult.data,
                        queryState
                    )
                    _historyState.update {
                        it.copy(
                            isRefreshing = false,
                            filteredEpisodeHistoryResults = Resource.Success(filteredMap),
                            episodeHistoryResults = allEpisodesResult,
                            pagination = pagination,
                            queryState = queryState.copy(page = pagination.current_page)
                        )
                    }
                }

                is Resource.Error -> _historyState.update {
                    it.copy(
                        isRefreshing = false,
                        filteredEpisodeHistoryResults = Resource.Error(allEpisodesResult.message),
                        episodeHistoryResults = allEpisodesResult,
                        pagination = defaultCompletePagination
                    )
                }

                is Resource.Loading -> {}
            }
        }
    }

    private fun applyFilters(updatedQueryState: EpisodeHistoryQueryState) {
        _historyState.update { it.copy(queryState = updatedQueryState) }
        if (_historyState.value.isEpisodeHistoryEmpty) return
        fetchHistory()
    }

    private fun changePage(page: Int) {
        viewModelScope.launch {
            val queryState = _historyState.value.queryState.copy(page = page)
            val currentResults = _historyState.value.episodeHistoryResults
            if (currentResults is Resource.Success) {
                val (filteredMap, pagination) = computeFilteredAndPaginatedData(
                    currentResults.data,
                    queryState
                )
                _historyState.update {
                    it.copy(
                        filteredEpisodeHistoryResults = Resource.Success(filteredMap),
                        pagination = pagination,
                        queryState = queryState.copy(page = pagination.current_page)
                    )
                }
            }
        }
    }

    private fun toggleEpisodeFavorite(episodeId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            val episode = repository.getCachedEpisodeDetailComplement(episodeId)
            if (episode == null) {
                handleError("Episode not found")
                return@launch
            }
            val updatedEpisode = episode.copy(isFavorite = isFavorite)
            repository.updateEpisodeDetailComplement(updatedEpisode)
            fetchHistory(isRefreshing = false)
        }
    }

    private fun toggleAnimeFavorite(malId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            val anime = repository.getCachedAnimeDetailComplementByMalId(malId)
            if (anime == null) {
                handleError("Anime not found")
                return@launch
            }
            val updatedAnime = anime.copy(isFavorite = isFavorite)
            repository.updateCachedAnimeDetailComplement(updatedAnime)
            fetchHistory(isRefreshing = false)
        }
    }

    private fun deleteEpisode(episodeId: String) {
        viewModelScope.launch {
            if (!repository.deleteEpisodeDetailComplement(episodeId)) {
                handleError("Episode not found")
                return@launch
            }
            fetchHistory(isRefreshing = false)
        }
    }

    private fun deleteAnime(malId: Int) {
        viewModelScope.launch {
            repository.deleteAnimeDetailById(malId)
            if (!repository.deleteAnimeDetailComplement(malId)) {
                handleError("Anime not found")
                return@launch
            }
            fetchHistory(isRefreshing = false)
        }
    }

    private suspend fun filterEpisodes(
        episodes: List<EpisodeDetailComplement>,
        queryState: EpisodeHistoryQueryState
    ): Map<AnimeDetailComplement, List<EpisodeDetailComplement>> {
        val filteredEpisodes = episodes
            .filter { episode ->
                queryState.isFavorite?.let { isFav -> episode.isFavorite == isFav } != false
            }
            .let { filtered ->
                if (queryState.searchQuery.isNotBlank()) {
                    AnimeTitleFinder.searchTitle(
                        searchQuery = queryState.searchQuery,
                        items = filtered,
                        extractors = episodeExtractors
                    )
                } else {
                    filtered
                }
            }

        return filteredEpisodes.groupBy { it.malId }
            .mapNotNull { (malId, episodes) ->
                ComplementUtils.getOrCreateAnimeDetailComplement(
                    repository = repository,
                    malId = malId
                )?.let { it to episodes }
            }.toMap()
    }

    private suspend fun computeFilteredAndPaginatedData(
        episodes: List<EpisodeDetailComplement>,
        queryState: EpisodeHistoryQueryState
    ): Pair<Map<AnimeDetailComplement, List<EpisodeDetailComplement>>, CompletePagination> {
        val filteredMap = filterEpisodes(episodes, queryState)
        if (filteredMap.isEmpty()) {
            return Pair(emptyMap(), defaultCompletePagination)
        }

        val totalEpisodes = filteredMap.values.sumOf { it.size }
        val lastVisiblePage =
            ceil(totalEpisodes.toDouble() / queryState.limit).toInt().coerceAtLeast(1)
        val adjustedPage = queryState.page.coerceAtMost(lastVisiblePage)
        val offset = (adjustedPage - 1) * queryState.limit
        val paginatedEpisodes =
            filteredMap.entries.flatMap { it.value }.drop(offset).take(queryState.limit)
        val paginatedMap = paginatedEpisodes.groupBy { it.malId }
            .mapNotNull { (malId, episodes) ->
                filteredMap.keys.find { it.malId == malId }?.let { it to episodes }
            }.toMap()

        return Pair(
            paginatedMap,
            CompletePagination(
                last_visible_page = lastVisiblePage,
                has_next_page = adjustedPage < lastVisiblePage,
                current_page = adjustedPage,
                items = Items(
                    count = paginatedMap.values.sumOf { it.size },
                    total = totalEpisodes,
                    per_page = queryState.limit
                )
            )
        )
    }

    private fun handleError(message: String) {
        _historyState.update {
            it.copy(
                filteredEpisodeHistoryResults = Resource.Error(message),
                episodeHistoryResults = Resource.Error(message),
            )
        }
    }
}