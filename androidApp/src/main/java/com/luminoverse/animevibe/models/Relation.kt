package com.luminoverse.animevibe.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Relation(
    val relation: String,
    val entry: List<CommonIdentity>
): Parcelable
