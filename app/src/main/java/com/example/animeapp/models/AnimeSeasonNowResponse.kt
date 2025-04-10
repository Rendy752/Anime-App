package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeSeasonNowResponse(
    val pagination: CompletePagination,
    val data: List<AnimeDetail>
)

val animeSeasonNowResponsePlaceholder = AnimeSeasonNowResponse(
    pagination = defaultCompletePagination,
    data = List(10) { animeDetailPlaceholder }
)