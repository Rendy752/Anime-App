package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeAniwatchSearchResponse(
    val animes: List<AnimeAniwatch>
)

@Serializable
data class AnimeAniwatch(
    val id: String,
    val name: String
)