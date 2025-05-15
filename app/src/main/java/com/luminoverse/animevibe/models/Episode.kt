package com.luminoverse.animevibe.models

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
    episodeId = "lorem-ipsum-123?ep=123",
    filler = false
)