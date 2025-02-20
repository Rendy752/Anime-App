package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Pagination(
    val last_visible_page: Int,
    val has_next_page: Boolean
)

@Serializable
data class CompletePagination(
    val last_visible_page: Int,
    val has_next_page: Boolean,
    val current_page: Int,
    val items: Items
)

@Serializable
data class Items(
    val count: Int,
    val total: Int,
    val per_page: Int
)