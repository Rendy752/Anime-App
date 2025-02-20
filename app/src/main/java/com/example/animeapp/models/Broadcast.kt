package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Broadcast(
    val day: String?,
    val time: String?,
    val timezone: String?,
    val string: String?
)