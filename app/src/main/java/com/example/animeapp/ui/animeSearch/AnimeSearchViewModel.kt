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

@HiltViewModel
class AnimeSearchViewModel @Inject constructor(
    private val animeSearchRepository: AnimeSearchRepository
) : ViewModel() {

    private val _animeSearchResults =
        MutableStateFlow<Resource<AnimeSearchResponse>>(Resource.Loading())
    val animeSearchResults: StateFlow<Resource<AnimeSearchResponse>> =
        _animeSearchResults.asStateFlow()

    private val _queryState = MutableStateFlow(AnimeSearchQueryState())
    val queryState: StateFlow<AnimeSearchQueryState> = _queryState.asStateFlow()

    private val _genres = MutableStateFlow<Resource<GenresResponse>>(Resource.Loading())
    val genres: StateFlow<Resource<GenresResponse>> = _genres.asStateFlow()

    private val _producers = MutableStateFlow<Resource<ProducersResponse>>(Resource.Loading())
    val producers: StateFlow<Resource<ProducersResponse>> = _producers.asStateFlow()

    private val _producersQueryState = MutableStateFlow(ProducersSearchQueryState())
    val producersQueryState: StateFlow<ProducersSearchQueryState> =
        _producersQueryState.asStateFlow()

    private val _selectedGenres = MutableStateFlow<List<Genre>>(emptyList())
    val selectedGenres: StateFlow<List<Genre>> = _selectedGenres.asStateFlow()

    private val _selectedProducers = MutableStateFlow<List<Producer>>(emptyList())
    val selectedProducers: StateFlow<List<Producer>> = _selectedProducers.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        fetchGenres()
        fetchProducers()
    }

    fun searchAnime() = viewModelScope.launch {
        if (queryState.value.query.isBlank() && queryState.value.isDefault() && queryState.value.isGenresDefault() && queryState.value.isProducersDefault()) {
            getRandomAnime()
        } else {
            _isRefreshing.value = true
            _animeSearchResults.value = Resource.Loading()
            _animeSearchResults.value = animeSearchRepository.searchAnime(queryState.value)
            _isRefreshing.value = false
        }
    }

    fun applyFilters(updatedQueryState: AnimeSearchQueryState) {
        _queryState.value = updatedQueryState
        viewModelScope.launch {
            searchAnime()
        }
    }

    private fun getRandomAnime() = viewModelScope.launch {
        _isRefreshing.value = true
        _animeSearchResults.value = Resource.Loading()
        _animeSearchResults.value = animeSearchRepository.getRandomAnime()
        _isRefreshing.value = false
    }

    fun fetchGenres() = viewModelScope.launch {
        _genres.value = Resource.Loading()
        _genres.value = animeSearchRepository.getGenres()
    }

    fun setSelectedGenre(genre: Genre) {
        _selectedGenres.update { currentList ->
            if (currentList.contains(genre)) {
                currentList.filter { it != genre }
            } else {
                currentList + genre
            }
        }
    }

    fun applyGenreFilters() {
        val genreIds = selectedGenres.value.joinToString(",") { it.mal_id.toString() }
        if (genreIds.isBlank()) {
            resetGenreSelection()
            return
        }
        applyFilters(queryState.value.defaultLimitAndPage().copy(genres = genreIds))
    }

    fun resetGenreSelection() {
        _selectedGenres.value = emptyList()
        applyFilters(queryState.value.resetGenres())
    }

    fun applyProducerQueryStateFilters(updatedQueryState: ProducersSearchQueryState) {
        _producersQueryState.value = updatedQueryState
        fetchProducers()
    }

    fun fetchProducers() = viewModelScope.launch {
        _producers.value = Resource.Loading()
        _producers.value = animeSearchRepository.getProducers(producersQueryState.value)
    }

    fun setSelectedProducer(producer: Producer) {
        _selectedProducers.update { currentList ->
            if (currentList.contains(producer)) {
                currentList.filter { it != producer }
            } else {
                currentList + producer
            }
        }
    }

    fun applyProducerFilters() {
        val producerIds = selectedProducers.value.joinToString(",") { it.mal_id.toString() }
        if (producerIds.isBlank()) {
            resetProducerSelection()
            return
        }
        applyFilters(queryState.value.defaultLimitAndPage().copy(producers = producerIds))
    }

    fun resetProducerSelection() {
        _selectedProducers.value = emptyList()
        producersQueryState.value.resetProducers()
        applyFilters(queryState.value.resetProducers())
    }

    fun resetBottomSheetFilters() {
        _queryState.value = queryState.value.resetBottomSheetFilters()
        searchAnime()
    }
}