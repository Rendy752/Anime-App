package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val file: String,
    val label: String? = null,
    val kind: String? = null,
    val default: Boolean? = false
)