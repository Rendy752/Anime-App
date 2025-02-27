package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeSourcesResponse(
    val tracks: List<Track>,
    val intro: TimeRange?,
    val outro: TimeRange?,
    val sources: List<Source>,
    val anilistID: Int,
    val malID: Int
)
