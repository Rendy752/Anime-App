package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class ListAnimeDetailResponse(
    val pagination: CompletePagination,
    val data: List<AnimeDetail>
)

val listAnimeDetailResponsePlaceholder = ListAnimeDetailResponse(
    pagination = defaultCompletePagination,
    data = List(10) { animeDetailPlaceholder }
)