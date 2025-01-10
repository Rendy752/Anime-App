package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeRecommendationResponse(
    val data: List<AnimeRecommendation>,
    val pagination: Pagination
)

