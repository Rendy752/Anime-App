package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.RetrofitInstance
import com.example.animeapp.data.local.database.AnimeRecommendationsDatabase

class AnimeRecommendationsRepository(
    val db: AnimeRecommendationsDatabase
) {
    suspend fun getAnimeRecommendations(page: Int = 1) =
        RetrofitInstance.api.getAnimeRecommendations(page)
}