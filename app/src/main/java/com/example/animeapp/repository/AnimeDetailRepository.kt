package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.RetrofitInstance
import com.example.animeapp.data.local.database.AnimeDetailDatabase

class AnimeDetailRepository(
    val db: AnimeDetailDatabase
) {
    suspend fun getAnimeDetail(id: Int) =
        RetrofitInstance.api.getAnimeDetail(id)
}
