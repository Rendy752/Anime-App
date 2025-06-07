package com.luminoverse.animevibe.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Track(
    val file: String,
    val label: String? = null,
    val kind: String? = null,
    val default: Boolean? = false
): Parcelable

val trackPlaceholder = Track(
    file = "",
    label = "None",
    kind = "captions",
    default = false
)