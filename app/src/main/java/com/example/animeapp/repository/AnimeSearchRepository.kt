package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.data.remote.api.RetrofitInstance

class AnimeSearchRepository(
    private val api: AnimeAPI = RetrofitInstance.api
) {
    suspend fun searchAnime(
        query: String,
        page: Int? = 1,
        limit: Int? = 10,
        type: String? = null,
        score: Double? = null,
        minScore: Double? = null,
        maxScore: Double? = null,
        status: String? = null,
        rating: String? = null,
        sfw: Boolean? = null,
        unapproved: Boolean? = null,
        genres: String? = null,
        genresExclude: String? = null,
        orderBy: String? = null,
        sort: String? = null,
        letter: String? = null,
        producers: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ) = api.getAnimeSearch(
        q = query,
        page = page,
        limit = limit,
        type = type,
        score = score,
        minScore = minScore,
        maxScore = maxScore,
        status = status,
        rating = rating,
        sfw = sfw,
        unapproved = unapproved,
        genres = genres,
        genresExclude = genresExclude,
        orderBy = orderBy,
        sort = sort,
        letter = letter,
        producers = producers,
        startDate = startDate,
        endDate = endDate
    )

    suspend fun getRandomAnime() = api.getRandomAnime()
}