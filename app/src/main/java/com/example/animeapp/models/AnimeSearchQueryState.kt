package com.example.animeapp.models

import com.example.animeapp.utils.Limit

data class AnimeSearchQueryState(
    val query: String = "",
    val page: Int = 1,
    val limit: Int? = Limit.DEFAULT_LIMIT,

    val type: String? = null,
    val score: Double? = null,
    val minScore: Double? = null,
    val maxScore: Double? = null,
    val status: String? = null,
    val rating: String? = null,
    val sfw: Boolean? = null,
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
        return query.isBlank() &&
                type == null &&
                score == null &&
                minScore == null &&
                maxScore == null &&
                status == null &&
                rating == null &&
                sfw == null &&
                unapproved == null &&
                genres == null &&
                genresExclude == null &&
                orderBy == null &&
                sort == null &&
                letter == null &&
                producers == null &&
                startDate == null &&
                endDate == null
    }
}