package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class CommonIdentity(
    val mal_id: Int,
    val type: String,
    val name: String,
    val url: String
): Parcelable
