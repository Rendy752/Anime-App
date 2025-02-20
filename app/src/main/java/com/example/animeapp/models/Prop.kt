package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Prop(
    val from: DateObject,
    val to: DateObject
)