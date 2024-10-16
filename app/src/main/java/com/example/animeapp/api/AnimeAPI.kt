package com.example.animeapp.api

import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.AnimeRecommendationResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AnimeAPI {
    @GET("v4/recommendations/anime")
    suspend fun getAnimeRecommendations(
        @Query("page") page: Int = 1,
    ): Response<AnimeRecommendationResponse>

    @GET("v4/anime/{id}/full")
    suspend fun getAnimeDetail(
        @Path("id") id: Int
    ): Response<AnimeDetailResponse>
}