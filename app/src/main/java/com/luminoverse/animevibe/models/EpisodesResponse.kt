package com.luminoverse.animevibe.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class EpisodesResponse(
    val totalEpisodes: Int,
    val episodes: List<Episode>
): Parcelable