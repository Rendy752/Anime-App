package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Aired(
    val from: String?,
    val to: String?,
    val prop: Prop,
    val string: String
)