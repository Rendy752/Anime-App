package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Theme(
    val openings: List<String>?,
    val endings: List<String>?
): Parcelable
