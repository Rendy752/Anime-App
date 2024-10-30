package com.example.animeappkotlin.data.remote.api

import com.example.animeappkotlin.models.AnimeDetailResponse
import com.example.animeappkotlin.models.AnimeRecommendationResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import retrofit2.Response
import java.io.IOException

class MockAnimeAPI(
    private val mockResponse: AnimeRecommendationResponse,
    private val latencyMillis: Long = 0 // Optional latency for simulating network delay
) : AnimeAPI {

    override suspend fun getAnimeRecommendations(page: Int): Response<AnimeRecommendationResponse> {
        delay(latencyMillis) // Simulate network delay if latencyMillis > 0
        return Response.success(mockResponse)
    }

    override fun getAnimeRecommendationsWrapper(page: Int): Response<AnimeRecommendationResponse> {
        // This is a wrapper for testing purposes, you can call the suspend function directly
        return runBlocking { getAnimeRecommendations(page) }
    }

    override suspend fun getAnimeDetail(id: Int): Response<AnimeDetailResponse> {
        // You can provide a mock AnimeDetailResponse here if needed
        throw IOException("Not implemented for MockAnimeAPI")
    }
}