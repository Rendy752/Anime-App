package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeSeasonNowResponse(
    val pagination: CompletePagination,
    val data: List<AnimeDetail>
)