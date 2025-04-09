package com.example.animeapp.models

data class AnimeSeasonNowSearchQueryState(
    val filter: String? = null,
    val sfw: Boolean? = true,
    val unapproved: Boolean? = null,
    val continuing: Boolean? = null,

    val page: Int = 1,
    val limit: Int? = 25,
) {
    private fun defaultLimitAndPage(): AnimeSeasonNowSearchQueryState {
        return copy(page = 1, limit = 25)
    }

    fun resetQueryState(): AnimeSeasonNowSearchQueryState {
        return defaultLimitAndPage().copy(
            filter = null,
            sfw = true,
            unapproved = null,
            continuing = null
        )
    }
}