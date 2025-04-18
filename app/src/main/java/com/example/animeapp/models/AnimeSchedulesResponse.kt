package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeSchedulesResponse(
    val pagination: CompletePagination,
    val data: List<AnimeDetail>
)

val animeSchedulesResponsePlaceholder = AnimeSchedulesResponse(
    pagination = defaultCompletePagination,
    data = List(10) { animeDetailPlaceholder }
)