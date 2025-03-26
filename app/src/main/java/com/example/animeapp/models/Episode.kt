package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Episode(
    val name: String,
    val episodeNo: Int,
    val episodeId: String,
    val filler: Boolean
): Parcelable

val episodePlaceholder = Episode(
    name = "Title of Episode",
    episodeNo = 1,
    episodeId = "watch-1",
    filler = false
)