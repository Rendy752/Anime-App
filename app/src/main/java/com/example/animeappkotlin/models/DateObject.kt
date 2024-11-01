package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class DateObject(
    val day: Int?,
    val month: Int?,
    val year: Int?
)