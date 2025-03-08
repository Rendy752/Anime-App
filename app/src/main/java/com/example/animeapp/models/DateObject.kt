package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class DateObject(
    val day: Int?,
    val month: Int?,
    val year: Int?
): Parcelable