package com.example.animeapp.ui.animeSearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.AnimeSearchQueryState
import com.example.animeapp.models.AnimeSearchResponse
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.models.GenresResponse
import com.example.animeapp.models.ProducersResponse
import com.example.animeapp.models.ProducersSearchQueryState
import com.example.animeapp.repository.AnimeSearchRepository
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
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

    private val _selectedGenreId = MutableStateFlow<List<Int>>(emptyList())
    val selectedGenreId: StateFlow<List<Int>> = _selectedGenreId.asStateFlow()

    private val _selectedProducerId = MutableStateFlow<List<Int>>(emptyList())
    val selectedProducerId: StateFlow<List<Int>> = _selectedProducerId.asStateFlow()

    init {
        getRandomAnime()
        fetchGenres()
        fetchProducers()
    }

    private fun searchAnime() = viewModelScope.launch {
        if (queryState.value.isDefault() && queryState.value.isGenresDefault() && queryState.value.isProducersDefault()) {
            getRandomAnime()
        } else {
            _animeSearchResults.value = Resource.Loading()
            val response = animeSearchRepository.searchAnime(queryState.value)
            _animeSearchResults.value = ResponseHandler.handleCommonResponse(response)
        }
    }

    fun applyFilters(updatedQueryState: AnimeSearchQueryState) {
        _queryState.value = updatedQueryState
        searchAnime()
    }

    private fun getRandomAnime() = viewModelScope.launch {
        _animeSearchResults.value = Resource.Loading()
        val response = animeSearchRepository.getRandomAnime()
        _animeSearchResults.value = handleAnimeRandomResponse(response)
    }

    private fun handleAnimeRandomResponse(response: Response<AnimeDetailResponse>): Resource<AnimeSearchResponse> {
        return ResponseHandler.handleResponse(response,
            onSuccess = { resultResponse ->
                AnimeSearchResponse(
                    data = listOf(resultResponse.data),
                    pagination = CompletePagination.default()
                )
            }
        )
    }

    fun fetchGenres() = viewModelScope.launch {
        _genres.value = Resource.Loading()
        val response = animeSearchRepository.getGenres()
        _genres.value = ResponseHandler.handleCommonResponse(response)
    }

    fun setSelectedGenreId(genreId: Int) {
        val currentList = _selectedGenreId.value.toMutableList()
        if (currentList.contains(genreId)) {
            currentList.remove(genreId)
        } else {
            currentList.add(genreId)
        }
        _selectedGenreId.value = currentList
    }

    fun applyGenreFilters() {
        val genreIds = selectedGenreId.value.joinToString(",")
        applyFilters(queryState.value.defaultLimitAndPage().copy(genres = genreIds))
    }

    fun resetGenreSelection() {
        _selectedGenreId.value = emptyList()
        applyFilters(queryState.value.resetGenres())
    }

    fun applyProducerQueryStateFilters(updatedQueryState: ProducersSearchQueryState) {
        _producersQueryState.value = updatedQueryState
        fetchProducers()
    }

    fun fetchProducers() = viewModelScope.launch {
        _producers.value = Resource.Loading()
        val response = animeSearchRepository.getProducers(producersQueryState.value)
        _producers.value = ResponseHandler.handleCommonResponse(response)
    }

    fun setSelectedProducerId(producerId: Int) {
        val currentList = _selectedProducerId.value.toMutableList()
        if (currentList.contains(producerId)) {
            currentList.remove(producerId)
        } else {
            currentList.add(producerId)
        }
        _selectedProducerId.value = currentList
    }

    fun applyProducerFilters() {
        val producerIds = selectedProducerId.value.joinToString(",")
        applyFilters(queryState.value.defaultLimitAndPage().copy(producers = producerIds))
    }

    fun resetProducerSelection() {
        _selectedProducerId.value = emptyList()
        producersQueryState.value.resetProducers()
        applyFilters(queryState.value.resetProducers())
    }

    fun resetBottomSheetFilters() {
        _queryState.value = queryState.value.resetBottomSheetFilters()
        searchAnime()
    }
}