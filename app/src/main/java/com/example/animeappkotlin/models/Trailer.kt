package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class Trailer(
    val youtube_id: String?,
    val url: String?,
    val embed_url: String?,
    val images: ImageUrl,
)
