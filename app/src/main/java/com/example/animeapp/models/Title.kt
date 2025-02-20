package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Title(
    val type: String,
    val title: String
)
