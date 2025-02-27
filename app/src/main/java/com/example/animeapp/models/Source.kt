package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Source(
    val url: String,
    val type: String
)