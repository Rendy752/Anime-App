package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeRecommendationResponse(
    val data: List<AnimeRecommendation>,
    val pagination: Pagination
)

