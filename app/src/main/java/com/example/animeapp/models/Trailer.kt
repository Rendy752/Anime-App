package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Trailer(
    val youtube_id: String?,
    val url: String?,
    val embed_url: String?,
    val images: ImageUrl,
): Parcelable
