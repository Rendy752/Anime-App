package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Prop(
    val from: DateObject,
    val to: DateObject
): Parcelable