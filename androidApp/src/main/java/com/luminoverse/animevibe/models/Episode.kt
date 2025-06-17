package com.luminoverse.animevibe.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Episode(
    val episode_no: Int,
    val id: String,
    val title: String,
    val japanese_title: String,
    val filler: Boolean
) : Parcelable

val episodePlaceholder = Episode(
    episode_no = 1,
    id = "lorem-ipsum-123?ep=123",
    title = "Title of Episode",
    japanese_title = "Japanese Title of Episode",
    filler = false
)