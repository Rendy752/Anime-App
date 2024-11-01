package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class Broadcast(
    val day: String?,
    val time: String?,
    val timezone: String?,
    val string: String?
)