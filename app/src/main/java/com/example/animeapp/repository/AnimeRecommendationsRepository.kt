package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.data.remote.api.RetrofitInstance

class AnimeRecommendationsRepository(
    private val api: AnimeAPI = RetrofitInstance.api
) {
    suspend fun getAnimeRecommendations(page: Int = 1) =
        api.getAnimeRecommendations(page)
}