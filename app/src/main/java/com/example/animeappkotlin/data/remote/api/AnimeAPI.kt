package com.example.animeappkotlin.data.remote.api

import com.example.animeappkotlin.models.AnimeDetailResponse
import com.example.animeappkotlin.models.AnimeRandomResponse
import com.example.animeappkotlin.models.AnimeRecommendationResponse
import com.example.animeappkotlin.models.AnimeSearchResponse
import kotlinx.coroutines.runBlocking
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AnimeAPI {
    @GET("v4/recommendations/anime")
    suspend fun getAnimeRecommendations(
        @Query("page") page: Int = 1,
    ): Response<AnimeRecommendationResponse>

    fun getAnimeRecommendationsWrapper(page: Int = 1): Response<AnimeRecommendationResponse> {
        return runBlocking { getAnimeRecommendations(page) }
    }

    @GET("v4/anime/{id}/full")
    suspend fun getAnimeDetail(
        @Path("id") id: Int
    ): Response<AnimeDetailResponse>

    @GET("/v4/random/anime")
    suspend fun getRandomAnime(): Response<AnimeRandomResponse>

    @GET("v4/anime")
    suspend fun getAnimeSearch(
        @Query("q") q: String,
        @Query("unapproved") unapproved: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("type") type: String? = null,
        @Query("score") score: Double? = null,
        @Query("min_score") min_score: Double? = null,
        @Query("max_score") max_score: Double? = null,
        @Query("status") status: String? = null,
        @Query("rating") rating: String? = null,
        @Query("sfw") sfw: Boolean? = null,
        @Query("genres") genres: String? = null,
        @Query("genres_exclude") genres_exclude: String? = null,
        @Query("order_by") order_by: String? = null,
        @Query("sort") sort: String? = null,
        @Query("letter") letter: String? = null,
        @Query("producers") producers: String? = null,
        @Query("start_date") start_date: String? = null,
        @Query("end_date") end_date: String? = null
    ): Response<AnimeSearchResponse>
}