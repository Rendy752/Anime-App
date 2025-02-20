package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Images(
    val jpg: ImageUrl,
    val webp: ImageUrl
)
