package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Episode(
    val name: String,
    val episodeNo: Int,
    val episodeId: String,
    val filler: Boolean
)