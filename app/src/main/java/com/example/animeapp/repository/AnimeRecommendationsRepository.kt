package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeRecommendationResponse
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import com.example.animeapp.utils.ResponseHandler.safeApiCall

class AnimeRecommendationsRepository(
    private val jikanAPI: AnimeAPI
) {
    suspend fun getAnimeRecommendations(page: Int = 1): Resource<AnimeRecommendationResponse> {
        val response = safeApiCall { jikanAPI.getAnimeRecommendations(page) }
        return ResponseHandler.handleCommonResponse(response)
    }
}