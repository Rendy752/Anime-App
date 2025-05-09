package com.example.animeapp.ui.animeSearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeSearchQueryState
import com.example.animeapp.models.AnimeSearchResponse
import com.example.animeapp.models.Genre
import com.example.animeapp.models.GenresResponse
import com.example.animeapp.models.Producer
import com.example.animeapp.models.ProducersResponse
import com.example.animeapp.models.ProducersSearchQueryState
import com.example.animeapp.repository.AnimeSearchRepository
import com.example.animeapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val animeSearchResults: Resource<AnimeSearchResponse> = Resource.Loading(),
    val queryState: AnimeSearchQueryState = AnimeSearchQueryState(),
    val genres: Resource<GenresResponse> = Resource.Loading(),
    val producers: Resource<ProducersResponse> = Resource.Loading(),
    val producersQueryState: ProducersSearchQueryState = ProducersSearchQueryState(),
    val isRefreshing: Boolean = false
)

data class FilterSelectionState(
    val selectedGenres: List<Genre> = emptyList(),
    val selectedProducers: List<Producer> = emptyList()
)

sealed class SearchAction {
    object SearchAnime : SearchAction()
    object FetchGenres : SearchAction()
    object FetchProducers : SearchAction()
    data class ApplyFilters(val updatedQueryState: AnimeSearchQueryState) : SearchAction()
    data class ApplyProducerQueryStateFilters(val updatedQueryState: ProducersSearchQueryState) : SearchAction()
    data class SetSelectedGenre(val genre: Genre) : SearchAction()
    data class SetSelectedProducer(val producer: Producer) : SearchAction()
    object ApplyGenreFilters : SearchAction()
    object ApplyProducerFilters : SearchAction()
    object ResetGenreSelection : SearchAction()
    object ResetProducerSelection : SearchAction()
    object ResetBottomSheetFilters : SearchAction()
}

@HiltViewModel
class AnimeSearchViewModel @Inject constructor(
    private val animeSearchRepository: AnimeSearchRepository
) : ViewModel() {

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _filterSelectionState = MutableStateFlow(FilterSelectionState())
    val filterSelectionState: StateFlow<FilterSelectionState> = _filterSelectionState.asStateFlow()

    init {
        onAction(SearchAction.FetchGenres)
        onAction(SearchAction.FetchProducers)
        onAction(SearchAction.SearchAnime)
    }

    fun onAction(action: SearchAction) {
        when (action) {
            SearchAction.SearchAnime -> searchAnime()
            SearchAction.FetchGenres -> fetchGenres()
            SearchAction.FetchProducers -> fetchProducers()
            is SearchAction.ApplyFilters -> applyFilters(action.updatedQueryState)
            is SearchAction.ApplyProducerQueryStateFilters -> applyProducerQueryStateFilters(action.updatedQueryState)
            is SearchAction.SetSelectedGenre -> setSelectedGenre(action.genre)
            is SearchAction.SetSelectedProducer -> setSelectedProducer(action.producer)
            SearchAction.ApplyGenreFilters -> applyGenreFilters()
            SearchAction.ApplyProducerFilters -> applyProducerFilters()
            SearchAction.ResetGenreSelection -> resetGenreSelection()
            SearchAction.ResetProducerSelection -> resetProducerSelection()
            SearchAction.ResetBottomSheetFilters -> resetBottomSheetFilters()
        }
    }

    private fun searchAnime() {
        viewModelScope.launch {
            val queryState = _searchState.value.queryState
            if (queryState.query.isBlank() && queryState.isDefault() && queryState.isGenresDefault() && queryState.isProducersDefault()) {
                getRandomAnime()
            } else {
                _searchState.update { it.copy(isRefreshing = true, animeSearchResults = Resource.Loading()) }
                val result = animeSearchRepository.searchAnime(queryState)
                _searchState.update { it.copy(isRefreshing = false, animeSearchResults = result) }
            }
        }
    }

    private fun getRandomAnime() {
        viewModelScope.launch {
            _searchState.update { it.copy(isRefreshing = true, animeSearchResults = Resource.Loading()) }
            val result = animeSearchRepository.getRandomAnime()
            _searchState.update { it.copy(isRefreshing = false, animeSearchResults = result) }
        }
    }

    private fun fetchGenres() {
        viewModelScope.launch {
            _searchState.update { it.copy(genres = Resource.Loading()) }
            val result = animeSearchRepository.getGenres()
            _searchState.update { it.copy(genres = result) }
        }
    }

    private fun fetchProducers() {
        viewModelScope.launch {
            _searchState.update { it.copy(producers = Resource.Loading()) }
            val result = animeSearchRepository.getProducers(_searchState.value.producersQueryState)
            _searchState.update { it.copy(producers = result) }
        }
    }

    private fun applyFilters(updatedQueryState: AnimeSearchQueryState) {
        _searchState.update { it.copy(queryState = updatedQueryState) }
        onAction(SearchAction.SearchAnime)
    }

    private fun applyProducerQueryStateFilters(updatedQueryState: ProducersSearchQueryState) {
        _searchState.update { it.copy(producersQueryState = updatedQueryState) }
        onAction(SearchAction.FetchProducers)
    }

    private fun setSelectedGenre(genre: Genre) {
        _filterSelectionState.update { currentState ->
            val currentList = currentState.selectedGenres
            if (currentList.contains(genre)) {
                currentState.copy(selectedGenres = currentList.filter { it != genre })
            } else {
                currentState.copy(selectedGenres = currentList + genre)
            }
        }
    }

    private fun setSelectedProducer(producer: Producer) {
        _filterSelectionState.update { currentState ->
            val currentList = currentState.selectedProducers
            if (currentList.contains(producer)) {
                currentState.copy(selectedProducers = currentList.filter { it != producer })
            } else {
                currentState.copy(selectedProducers = currentList + producer)
            }
        }
    }

    private fun applyGenreFilters() {
        val genreIds = _filterSelectionState.value.selectedGenres.joinToString(",") { it.mal_id.toString() }
        if (genreIds.isBlank()) {
            onAction(SearchAction.ResetGenreSelection)
            return
        }
        val updatedQueryState = _searchState.value.queryState.defaultLimitAndPage().copy(genres = genreIds)
        onAction(SearchAction.ApplyFilters(updatedQueryState))
    }

    private fun applyProducerFilters() {
        val producerIds = _filterSelectionState.value.selectedProducers.joinToString(",") { it.mal_id.toString() }
        if (producerIds.isBlank()) {
            onAction(SearchAction.ResetProducerSelection)
            return
        }
        val updatedQueryState = _searchState.value.queryState.defaultLimitAndPage().copy(producers = producerIds)
        onAction(SearchAction.ApplyFilters(updatedQueryState))
    }

    private fun resetGenreSelection() {
        _filterSelectionState.update { it.copy(selectedGenres = emptyList()) }
        val updatedQueryState = _searchState.value.queryState.resetGenres()
        onAction(SearchAction.ApplyFilters(updatedQueryState))
    }

    private fun resetProducerSelection() {
        _filterSelectionState.update { it.copy(selectedProducers = emptyList()) }
        _searchState.update { it.copy(producersQueryState = it.producersQueryState.resetProducers()) }
        val updatedQueryState = _searchState.value.queryState.resetProducers()
        onAction(SearchAction.ApplyFilters(updatedQueryState))
    }

    private fun resetBottomSheetFilters() {
        _searchState.update { it.copy(queryState = it.queryState.resetBottomSheetFilters()) }
        onAction(SearchAction.SearchAnime)
    }
}