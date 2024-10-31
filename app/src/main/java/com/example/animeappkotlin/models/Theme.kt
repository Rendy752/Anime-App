package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class Theme(
    val openings: List<String>?,
    val endings: List<String>?
)
