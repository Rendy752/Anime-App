package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Track(
    val file: String,
    val label: String? = null,
    val kind: String? = null,
    val default: Boolean? = false
): Parcelable

val trackPlaceholder = Track(
    file = "https://cdn.myanimelist.net/images/",
    label = "English",
    kind = "sub",
    default = true
)