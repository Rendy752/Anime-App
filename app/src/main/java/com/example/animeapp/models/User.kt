package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val url: String,
    val username: String
)
