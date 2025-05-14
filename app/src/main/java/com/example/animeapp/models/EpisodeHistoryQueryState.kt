package com.example.animeapp.models

data class EpisodeHistoryQueryState(
    val searchQuery: String = "",
    val isFavorite: Boolean? = null,
    val sortBy: SortBy = SortBy.NewestFirst,
    val page: Int = 1,
    val limit: Int = 10
) {
    enum class SortBy {
        NewestFirst, AnimeTitle, EpisodeTitle, EpisodeNumber
    }
}