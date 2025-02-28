package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class EpisodesResponse(
    val totalEpisodes: Int,
    val episodes: List<Episode>
)