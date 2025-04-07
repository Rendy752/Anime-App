package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeSearchQueryState
import com.example.animeapp.models.AnimeSearchResponse
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.models.GenresResponse
import com.example.animeapp.models.ProducersResponse
import com.example.animeapp.models.ProducersSearchQueryState
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import com.example.animeapp.utils.ResponseHandler.safeApiCall

class AnimeSearchRepository(
    private val jikanAPI: AnimeAPI
) {
    suspend fun searchAnime(
        queryState: AnimeSearchQueryState
    ): Resource<AnimeSearchResponse> {
        val response = safeApiCall {
            jikanAPI.getAnimeSearch(
                q = queryState.query,
                page = queryState.page,
                limit = queryState.limit,
                type = queryState.type,
                score = queryState.score,
                minScore = queryState.minScore,
                maxScore = queryState.maxScore,
                status = queryState.status,
                rating = queryState.rating,
                sfw = queryState.sfw,
                unapproved = queryState.unapproved,
                genres = queryState.genres,
                genresExclude = queryState.genresExclude,
                orderBy = queryState.orderBy,
                sort = queryState.sort,
                letter = queryState.letter,
                producers = queryState.producers,
                startDate = queryState.startDate,
                endDate = queryState.endDate
            )
        }
        return ResponseHandler.handleCommonResponse(response)
    }

    suspend fun getRandomAnime(): Resource<AnimeSearchResponse> {
        val response = safeApiCall { jikanAPI.getRandomAnime() }
        val resource = ResponseHandler.handleCommonResponse(response)
        if (resource is Resource.Success) {
            return Resource.Success(
                AnimeSearchResponse(
                    data = listOf(resource.data.data),
                    pagination = CompletePagination.Companion.default()
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
        val response = safeApiCall {
            jikanAPI.getProducers(
                page = queryState.page,
                limit = queryState.limit,
                q = queryState.query,
                orderBy = queryState.orderBy,
                sort = queryState.sort,
                letter = queryState.letter
            )
        }
        return ResponseHandler.handleCommonResponse(response)
    }
}