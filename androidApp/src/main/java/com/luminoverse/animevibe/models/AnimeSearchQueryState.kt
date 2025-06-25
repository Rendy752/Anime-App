package com.luminoverse.animevibe.models

data class AnimeSearchQueryState(
    val query: String = "",
    val page: Int = 1,
    val limit: Int? = 10,

    val type: String? = null,
    val score: Double? = null,
    val minScore: Double? = null,
    val maxScore: Double? = null,
    val status: String? = null,
    val rating: String? = null,
    val sfw: Boolean? = true,
    val unapproved: Boolean? = null,
    val genres: String? = null,
    val genresExclude: String? = null,
    val orderBy: String? = null,
    val sort: String? = null,
    val letter: String? = null,
    val producers: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
) {
    fun isDefault(): Boolean {
        return type == null &&
                score == null &&
                minScore == null &&
                maxScore == null &&
                status == null &&
                rating == null &&
                sfw == true &&
                unapproved == null &&
                orderBy == null &&
                sort == null &&
                letter == null &&
                startDate == null &&
                endDate == null
    }

    fun isGenresDefault(): Boolean {
        return genres == null && genresExclude == null
    }

    fun isProducersDefault(): Boolean {
        return producers == null
    }

    fun defaultLimitAndPage(): AnimeSearchQueryState {
        return copy(page = 1, limit = 10)
    }

    fun resetGenres(): AnimeSearchQueryState {
        return copy(
            page = 1, limit = 10, genres = null, genresExclude = null
        )
    }

    fun resetProducers(): AnimeSearchQueryState {
        return copy(
            page = 1, limit = 10, producers = null
        )
    }

    fun resetBottomSheetFilters(): AnimeSearchQueryState {
        return defaultLimitAndPage().copy(
            type = null,
            status = null,
            rating = null,
            score = null,
            minScore = null,
            maxScore = null,
            orderBy = null,
            sort = null,
            startDate = null,
            endDate = null,
            unapproved = null,
            sfw = true
        )
    }
}