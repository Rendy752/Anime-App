package com.example.animeapp.dataclass

data class ResponseWithPaginationResponse(
    val pagination: Pagination,
    val data: List<Any>
)

data class Pagination(
    val last_visible_page: Int,
    val has_next_page: Boolean
)
