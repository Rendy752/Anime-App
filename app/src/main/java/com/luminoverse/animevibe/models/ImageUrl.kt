package com.luminoverse.animevibe.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class ImageUrl(
    val image_url: String?,
    val small_image_url: String?,
    val medium_image_url: String?,
    val large_image_url: String?,
    val maximum_image_url: String?
): Parcelable

val imageUrlPlaceholder = ImageUrl(
    image_url = "",
    small_image_url = "",
    medium_image_url = "",
    large_image_url = "",
    maximum_image_url = ""
)