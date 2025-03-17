package com.example.animeapp.utils

import com.example.animeapp.models.AnimeSearchQueryState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FilterUtils {

    val TYPE_OPTIONS =
        listOf("Any", "TV", "Movie", "OVA", "Special", "ONA", "Music", "CM", "PV", "TV Special")
    val STATUS_OPTIONS = listOf("Any", "Airing", "Complete", "Upcoming")
    val RATING_OPTIONS = listOf("Any", "G", "PG", "PG13", "R17", "R", "Rx")
    private val RATING_DESCRIPTIONS = mapOf(
        "G" to "All Ages",
        "PG" to "Children",
        "PG13" to "Teens 13 or older",
        "R17" to "17+ (violence & profanity)",
        "R" to "Mild Nudity",
        "Rx" to "Hentai"
    )
    val ORDER_BY_OPTIONS = listOf(
        "Any", "mal_id", "title", "start_date", "end_date", "episodes", "score",
        "scored_by", "rank", "popularity", "members", "favorites"
    )
    val SORT_OPTIONS = listOf("Any", "desc", "asc")

    fun collectFilterValues(
        currentState: AnimeSearchQueryState,
        type: String?,
        score: Double?,
        minScore: Double?,
        maxScore: Double?,
        status: String?,
        rating: String?,
        sfw: Boolean?,
        unapproved: Boolean?,
        orderBy: String?,
        sort: String?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): AnimeSearchQueryState {
        return currentState.defaultLimitAndPage().copy(
            type = type?.takeIf { it != "Any" },
            score = if (minScore != null || maxScore != null) {
                null
            } else {
                score
            },
            minScore = minScore,
            maxScore = maxScore,
            status = status?.takeIf { it != "Any" },
            rating = rating?.takeIf { it != "Any" },
            sfw = sfw,
            unapproved = unapproved,
            orderBy = orderBy?.takeIf { it != "Any" },
            sort = sort?.takeIf { it != "Any" },
            startDate = startDate?.let { formatDate(it) },
            endDate = endDate?.let { formatDate(it) }
        )
    }

    fun getRatingDescription(ratingCode: String): String {
        return RATING_DESCRIPTIONS[ratingCode] ?: ratingCode
    }

    private fun formatDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        return date.format(formatter)
    }
}