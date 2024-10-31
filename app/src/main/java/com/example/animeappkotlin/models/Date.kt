package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class Aired(
    val from: String,
    val to: String,
    val prop: Prop,
    val string: String
)

@Serializable
data class Prop(
    val from: DateObject,
    val to: DateObject
)

@Serializable
data class DateObject(
    val day: Int,
    val month: Int,
    val year: Int
)

@Serializable
data class Broadcast(
    val day: String?,
    val time: String?,
    val timezone: String?,
    val string: String?
)