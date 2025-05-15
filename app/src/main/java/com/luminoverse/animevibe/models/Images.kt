package com.luminoverse.animevibe.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Images(
    val jpg: ImageUrl,
    val webp: ImageUrl
): Parcelable
