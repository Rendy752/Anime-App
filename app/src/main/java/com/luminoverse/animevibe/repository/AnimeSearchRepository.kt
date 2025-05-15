package com.luminoverse.animevibe.repository

import com.luminoverse.animevibe.data.local.dao.GenreDao
import com.luminoverse.animevibe.data.remote.api.AnimeAPI
import com.luminoverse.animevibe.models.AnimeSearchQueryState
import com.luminoverse.animevibe.models.AnimeSearchResponse
import com.luminoverse.animevibe.models.Genre
import com.luminoverse.animevibe.models.GenresResponse
import com.luminoverse.animevibe.models.ProducerResponse
import com.luminoverse.animevibe.models.ProducersResponse
import com.luminoverse.animevibe.models.ProducersSearchQueryState
import com.luminoverse.animevibe.models.defaultCompletePagination
import com.luminoverse.animevibe.utils.Resource
import com.luminoverse.animevibe.utils.ResponseHandler
import com.luminoverse.animevibe.utils.ResponseHandler.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnimeSearchRepository(
    private val jikanAPI: AnimeAPI,
    private val genreDao: GenreDao
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

    suspend fun getProducer(malId: Int): Resource<ProducerResponse> {
        val response = safeApiCall { jikanAPI.getProducer(malId) }
        return ResponseHandler.handleCommonResponse(response)
    }

    suspend fun getCachedGenres(): List<Genre> =
        withContext(Dispatchers.IO) {
            genreDao.getGenres()
        }

    suspend fun insertCachedGenre(genre: Genre) =
        withContext(Dispatchers.IO) {
            genreDao.insertGenre(genre)
        }
}