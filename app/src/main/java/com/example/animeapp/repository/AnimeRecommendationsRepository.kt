package com.example.animeappkotlin.repository

import com.example.animeappkotlin.data.remote.api.RetrofitInstance
import com.example.animeappkotlin.data.local.database.AnimeRecommendationsDatabase

class AnimeRecommendationsRepository(
    val db: AnimeRecommendationsDatabase
) {
    suspend fun getAnimeRecommendations(page: Int = 1) =
        RetrofitInstance.api.getAnimeRecommendations(page)
}