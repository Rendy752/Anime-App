package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeHistoryQueryState(
    val searchQuery: String = "",
    val isFavorite: Boolean? = null,
    val sortBy: SortBy = SortBy.LastWatchedDesc,
    val page: Int = 1,
    val limit: Int = 10
) {
    enum class SortBy {
        LastWatchedDesc,
        LastWatchedAsc,
        AnimeTitleAsc,
        AnimeTitleDesc,
        EpisodeTitleAsc,
        EpisodeTitleDesc
    }

    fun isDefault(): Boolean {
        return searchQuery.isEmpty() && isFavorite == null && sortBy == SortBy.LastWatchedDesc && page == 1 && limit == 25
    }

    fun resetFilters(): EpisodeHistoryQueryState {
        return copy(searchQuery = "", isFavorite = null, sortBy = SortBy.LastWatchedDesc, page = 1)
    }
}