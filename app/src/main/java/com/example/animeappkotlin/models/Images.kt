package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class Images(
    val jpg: ImageUrl,
    val webp: ImageUrl
)

@Serializable
data class ImageUrl(
    val image_url: String,
    val small_image_url: String,
    val medium_image_url: String?,
    val large_image_url: String,
    val maximum_image_url: String?
)
