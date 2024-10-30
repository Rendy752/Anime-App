package com.example.animeappkotlin.repository

import com.example.animeappkotlin.data.local.database.AnimeRecommendationsDatabase
import com.example.animeappkotlin.data.remote.api.AnimeAPI
import com.example.animeappkotlin.data.remote.api.RetrofitInstance

class AnimeRecommendationsRepository(
    private val api: AnimeAPI = RetrofitInstance.api,
    val db: AnimeRecommendationsDatabase
) {
    suspend fun getAnimeRecommendations(page: Int = 1) =
        api.getAnimeRecommendations(page)
}