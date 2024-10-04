package com.example.animeapp.models

data class ResponseWithPaginationResponse(
    val pagination: Pagination,
    val data: List<AnimeRecommendation>
)

data class Pagination(
    val last_visible_page: Int,
    val has_next_page: Boolean
)
