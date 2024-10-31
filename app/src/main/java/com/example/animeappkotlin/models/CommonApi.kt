package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeRecommendationResponse(
    val pagination: Pagination,
    val data: List<AnimeRecommendation>
)

@Serializable
data class Pagination(
    val last_visible_page: Int,
    val has_next_page: Boolean
)

@Serializable
data class AnimeDetailResponse(
    val data: AnimeDetail
)
