package com.luminoverse.animevibe.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Stable
data class Source(
    val url: String,
    val type: String
) : Parcelable

val sourcePlaceholder = Source(
    url = "http://example.com",
    type = "captions"
)