package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class EpisodeSourcesResponse(
    val tracks: List<Track>,
    val intro: TimeRange?,
    val outro: TimeRange?,
    val sources: List<Source>,
    val anilistID: Int,
    val malID: Int
) : Parcelable

val episodeSourcesResponsePlaceholder = EpisodeSourcesResponse(
    tracks = listOf(trackPlaceholder),
    intro = timeRangePlaceholder,
    outro = timeRangePlaceholder,
    sources = listOf(sourcePlaceholder),
    anilistID = 1,
    malID = 1735
)
