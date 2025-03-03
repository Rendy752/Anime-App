package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Broadcast(
    val day: String?,
    val time: String?,
    val timezone: String?,
    val string: String?
): Parcelable