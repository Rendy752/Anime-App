package com.example.animeapp.models

data class EpisodeHistoryQueryState(
    val searchQuery: String = "",
    val isFavorite: Boolean? = null,
    val sortBy: SortBy = SortBy.LastWatched,
    val isAscending: Boolean = false,
    val page: Int = 1,
    val limit: Int = 10
) {
    enum class SortBy {
        LastWatched, AnimeTitle, EpisodeTitle
    }
}