package com.example.animeapp.repository

import com.example.animeapp.api.RetrofitInstance
import com.example.animeapp.db.AnimeDetailDatabase
import com.example.animeapp.models.AnimeDetailResponse
import retrofit2.Response

class AnimeDetailRepository(
    val db: AnimeDetailDatabase
) {
    suspend fun getAnimeDetail(id: Int) =
        RetrofitInstance.api.getAnimeDetail(id)
}
