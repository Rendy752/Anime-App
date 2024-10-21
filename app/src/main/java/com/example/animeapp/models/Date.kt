package com.example.animeapp.models

data class Aired(
    val from: String,
    val to: String,
    val prop: Prop,
    val string: String
)

data class Prop(
    val from: DateObject,
    val to: DateObject
)

data class DateObject(
    val day: Int,
    val month: Int,
    val year: Int
)

data class Broadcast(
    val day: String?,
    val time: String?,
    val timezone: String?,
    val string: String?
)