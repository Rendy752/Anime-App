package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeDetailResponse(
    val data: AnimeDetail
)