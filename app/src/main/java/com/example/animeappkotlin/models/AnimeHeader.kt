package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeHeader(
    val mal_id: Int,
    val url: String,
    val images: Images,
    val title: String
)
