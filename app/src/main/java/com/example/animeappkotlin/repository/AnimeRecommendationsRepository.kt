package com.example.animeappkotlin.repository

import com.example.animeappkotlin.data.remote.api.AnimeAPI
import com.example.animeappkotlin.data.remote.api.RetrofitInstance

class AnimeRecommendationsRepository(
    private val api: AnimeAPI = RetrofitInstance.api
) {
    suspend fun getAnimeRecommendations(page: Int = 1) =
        api.getAnimeRecommendations(page)
}