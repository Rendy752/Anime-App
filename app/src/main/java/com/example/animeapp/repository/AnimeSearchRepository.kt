package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeSearchQueryState
import com.example.animeapp.models.AnimeSearchResponse
import com.example.animeapp.models.GenresResponse
import com.example.animeapp.models.ProducersResponse
import com.example.animeapp.models.ProducersSearchQueryState
import com.example.animeapp.models.defaultCompletePagination
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import com.example.animeapp.utils.ResponseHandler.safeApiCall

class AnimeSearchRepository(
    private val jikanAPI: AnimeAPI
) {
    suspend fun searchAnime(
        queryState: AnimeSearchQueryState
    ): Resource<AnimeSearchResponse> {
        queryState.apply {
            val response = safeApiCall {
                jikanAPI.getAnimeSearch(
                    q = query,
                    page = page,
                    limit = limit,
                    type = type,
                    score = score,
                    minScore = minScore,
                    maxScore = maxScore,
                    status = status,
                    rating = rating,
                    sfw = sfw,
                    unapproved = unapproved,
                    genres = genres,
                    genresExclude = genresExclude,
                    orderBy = orderBy,
                    sort = sort,
                    letter = letter,
                    producers = producers,
                    startDate = startDate,
                    endDate = endDate
                )
            }
            return ResponseHandler.handleCommonResponse(response)
        }
    }

    suspend fun getRandomAnime(): Resource<AnimeSearchResponse> {
        val response = safeApiCall { jikanAPI.getRandomAnime() }
        val resource = ResponseHandler.handleCommonResponse(response)
        if (resource is Resource.Success) {
            return Resource.Success(
                AnimeSearchResponse(
                    data = listOf(resource.data.data),
                    pagination = defaultCompletePagination
                )
            )
        }
        return Resource.Error(resource.message ?: "An error occurred")
    }

    suspend fun getGenres(): Resource<GenresResponse> {
        val response = safeApiCall { jikanAPI.getGenres() }
        return ResponseHandler.handleCommonResponse(response)
    }

    suspend fun getProducers(
        queryState: ProducersSearchQueryState
    ): Resource<ProducersResponse> {
        queryState.apply {
            val response = safeApiCall {
                jikanAPI.getProducers(
                    page = page,
                    limit = limit,
                    q = query,
                    orderBy = orderBy,
                    sort = sort,
                    letter = letter
                )
            }
            return ResponseHandler.handleCommonResponse(response)
        }
    }
}