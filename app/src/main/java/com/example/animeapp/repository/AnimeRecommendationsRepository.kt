package com.example.animeapp.repository

import com.example.animeapp.api.RetrofitInstance
import com.example.animeapp.db.AnimeRecommendationsDatabase

class AnimeRecommendationsRepository(
    val db: AnimeRecommendationsDatabase
) {
    suspend fun getAnimeRecommendations(page: Int = 1) =
        RetrofitInstance.api.getAnimeRecommendations(page)
}