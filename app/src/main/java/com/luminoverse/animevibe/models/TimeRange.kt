package com.luminoverse.animevibe.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class TimeRange(
    val start: Long,
    val end: Long
): Parcelable

val timeRangePlaceholder = TimeRange(
    start = 0,
    end = 0
)