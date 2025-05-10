package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Pagination(
    val last_visible_page: Int,
    val has_next_page: Boolean
)

val defaultPagination = Pagination(
    last_visible_page = 1,
    has_next_page = false
)

@Serializable
data class CompletePagination(
    val last_visible_page: Int,
    val has_next_page: Boolean,
    val current_page: Int,
    val items: Items
)

val defaultCompletePagination = CompletePagination(
    last_visible_page = 1,
    has_next_page = false,
    current_page = 1,
    items = Items.default()
)

@Serializable
data class Items(
    val count: Int,
    val total: Int,
    val per_page: Int
) {
    companion object {
        fun default(): Items {
            return Items(count = 0, total = 0, per_page = 25)
        }
    }
}