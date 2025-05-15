package com.luminoverse.animevibe.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Aired(
    val from: String?,
    val to: String?,
    val prop: Prop,
    val string: String
): Parcelable