package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Relation(
    val relation: String,
    val entry: List<CommonIdentity>
)
