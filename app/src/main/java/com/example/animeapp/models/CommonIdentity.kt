package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class CommonIdentity(
    val mal_id: Int,
    val type: String,
    val name: String,
    val url: String
)
