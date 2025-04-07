package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class WatchRecentEpisodeResponse(
    val data: List<WatchRecentEpisode>,
    val pagination: Pagination
)

@Serializable
data class WatchRecentEpisode(
    val entry: AnimeHeader,
    val episodes: List<RecentEpisodeDetail>,
    val region_locked: Boolean,
)

@Serializable
data class RecentEpisodeDetail(
    val mal_id: Int,
    val url: String,
    val title: String,
    val premium: Boolean,
)
