package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeRandomResponse(
    val data: AnimeDetail,
)