package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class Prop(
    val from: DateObject,
    val to: DateObject
)