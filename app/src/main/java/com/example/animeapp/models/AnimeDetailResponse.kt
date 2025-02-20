package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeDetailResponse(
    val data: AnimeDetail
)