package com.luminoverse.animevibe.repository

import com.luminoverse.animevibe.data.remote.api.AnimeAPI
import com.luminoverse.animevibe.models.AnimeRecommendationResponse
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.resource.ResponseHandler
import com.luminoverse.animevibe.utils.resource.ResponseHandler.safeApiCall

class AnimeRecommendationsRepository(
    private val jikanAPI: AnimeAPI
) {
    suspend fun getAnimeRecommendations(page: Int = 1): Resource<AnimeRecommendationResponse> {
        val response = safeApiCall { jikanAPI.getAnimeRecommendations(page) }
        return ResponseHandler.handleCommonResponse(response)
    }
}