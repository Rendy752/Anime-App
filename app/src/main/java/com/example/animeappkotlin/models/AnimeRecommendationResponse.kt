package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeRecommendationResponse(
    val pagination: Pagination,
    val data: List<AnimeRecommendation>
)

