package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.data.remote.api.RetrofitInstance

class AnimeSearchRepository(
    private val api: AnimeAPI = RetrofitInstance.api
) {
    suspend fun searchAnime(query: String, page: Int? = 1, limit: Int? = 10) =
        api.getAnimeSearch(q = query, page = page, limit = limit)

    suspend fun getRandomAnime() = api.getRandomAnime()
}