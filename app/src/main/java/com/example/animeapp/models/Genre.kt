package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Genre(
    val mal_id: Int,
    val name: String,
    val url: String,
    val count: Int
): Parcelable

val genrePlaceholder = Genre(
    mal_id = 0,
    name = "",
    url = "",
    count = 0
)