package com.example.animeappkotlin.ui.AnimeSearch

import AnimeSearchQueryState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeappkotlin.models.AnimeRandomResponse
import com.example.animeappkotlin.models.AnimeSearchResponse
import com.example.animeappkotlin.models.CompletePagination
import com.example.animeappkotlin.models.Items
import com.example.animeappkotlin.repository.AnimeSearchRepository
import com.example.animeappkotlin.utils.Limit
import com.example.animeappkotlin.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class AnimeSearchViewModel(
    private val animeSearchRepository: AnimeSearchRepository
) : ViewModel() {

    private val _animeSearchResults =
        MutableStateFlow<Resource<AnimeSearchResponse>>(Resource.Loading())
    val animeSearchResults: StateFlow<Resource<AnimeSearchResponse>> =
        _animeSearchResults.asStateFlow()

    private val _queryState = MutableStateFlow(AnimeSearchQueryState())
    val queryState: StateFlow<AnimeSearchQueryState> = _queryState.asStateFlow()

    fun updateQuery(query: String) {
        _queryState.value = queryState.value.copy(query = query, page = 1)
        searchAnime()
    }

    fun updatePage(page: Int) {
        _queryState.value = queryState.value.copy(page = page)
        searchAnime()
    }

    fun updateLimit(limit: Int?) {
        if (_queryState.value.limit != limit) {
            _queryState.value = queryState.value.copy(limit = limit, page = 1)
            searchAnime()
        }
    }

    init {
        getRandomAnime()
    }

    fun searchAnime() = viewModelScope.launch {
        if (queryState.value.query.isBlank()) {
            getRandomAnime()
        } else {
            _animeSearchResults.value = Resource.Loading()
            val response = animeSearchRepository.searchAnime(
                queryState.value.query,
                queryState.value.page,
                queryState.value.limit ?: Limit.DEFAULT_LIMIT
            )
            _animeSearchResults.value = handleAnimeSearchResponse(response)
        }
    }

    fun getRandomAnime() = viewModelScope.launch {
        _animeSearchResults.value = Resource.Loading()
        val response = animeSearchRepository.getRandomAnime()
        _animeSearchResults.value = handleAnimeRandomResponse(response)
    }

    private fun handleAnimeSearchResponse(response: Response<AnimeSearchResponse>): Resource<AnimeSearchResponse> {
        return if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                Resource.Success(resultResponse)
            } ?: Resource.Error("Response body is null")
        } else {
            Resource.Error(response.message())
        }
    }

    private fun handleAnimeRandomResponse(response: Response<AnimeRandomResponse>): Resource<AnimeSearchResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                val searchResponse = AnimeSearchResponse(
                    data = listOf(resultResponse.data),
                    pagination = CompletePagination(
                        last_visible_page = 1,
                        has_next_page = false,
                        current_page = 1,
                        items = Items(
                            count = 1,
                            total = 1,
                            per_page = 1
                        )
                    )
                )
                return Resource.Success(searchResponse)
            } ?: return Resource.Error("Response body is null")
        } else {
            return Resource.Error(response.message())
        }
    }
}