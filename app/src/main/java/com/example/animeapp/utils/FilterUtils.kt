package com.example.animeapp.utils

import com.example.animeapp.models.AnimeSearchQueryState
import com.example.animeapp.models.Episode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FilterUtils {

    data class FilterState(
        val queryState: AnimeSearchQueryState,
        val type: String = queryState.type ?: "Any",
        val score: String? = queryState.score?.toString(),
        val minScore: String? = queryState.minScore?.toString(),
        val maxScore: String? = queryState.maxScore?.toString(),
        val status: String = queryState.status ?: "Any",
        val rating: String = queryState.rating ?: "Any",
        val sfw: Boolean = queryState.sfw == true,
        val unapproved: Boolean = queryState.unapproved == true,
        val orderBy: String = queryState.orderBy ?: "Any",
        val sort: String = queryState.sort ?: "Any",
        val enableDateRange: Boolean = queryState.startDate != null || queryState.endDate != null,
        val startDate: LocalDate? = queryState.startDate?.let { LocalDate.parse(it) },
        val endDate: LocalDate? = queryState.endDate?.let { LocalDate.parse(it) }
    )

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
        endDate: LocalDate?,
        enableDateRange: Boolean
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
            sfw = sfw.takeIf { it != false },
            unapproved = unapproved.takeIf { it != false },
            orderBy = orderBy?.takeIf { it != "Any" },
            sort = sort?.takeIf { it != "Any" },
            startDate = if (enableDateRange) startDate?.let { formatDate(it) } else null,
            endDate = if (enableDateRange) endDate?.let { formatDate(it) } else null
        )
    }

    fun getRatingDescription(ratingCode: String): String {
        return RATING_DESCRIPTIONS[ratingCode] ?: ratingCode
    }

    private fun formatDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        return date.format(formatter)
    }

    fun filterEpisodes(episodes: List<Episode>, query: String): List<Episode> {
        return if (query.isBlank()) {
            episodes
        } else {
            episodes.filter { episode ->
                episode.episodeNo.toString().contains(query, ignoreCase = true) ||
                        episode.name.contains(query, ignoreCase = true)
            }
        }
    }
}