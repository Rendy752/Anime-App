package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class Title(
    val type: String,
    val title: String
)
