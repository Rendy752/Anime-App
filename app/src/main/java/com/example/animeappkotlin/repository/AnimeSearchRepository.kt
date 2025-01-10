package com.example.animeappkotlin.repository

import com.example.animeappkotlin.data.remote.api.AnimeAPI
import com.example.animeappkotlin.data.remote.api.RetrofitInstance

class AnimeSearchRepository(
    private val api: AnimeAPI = RetrofitInstance.api
) {
    suspend fun searchAnime(query: String) =
        api.getAnimeSearch(q = query)
}