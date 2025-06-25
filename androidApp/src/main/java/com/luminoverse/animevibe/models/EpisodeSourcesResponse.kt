package com.luminoverse.animevibe.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Stable
data class EpisodeSourcesResponse(
    val streamingLink: EpisodeSources,
    val servers: List<EpisodeServer>
) : Parcelable

@Parcelize
@Serializable
@Stable
data class EpisodeSources(
    val id: String,
    val type: String,
    val link: Source,
    val tracks: List<Track>,
    val intro: TimeRange,
    val outro: TimeRange,
    val server: String
) : Parcelable

val episodeSourcesPlaceholder = EpisodeSources(
    id = "1156440",
    type = "sub",
    link = sourcePlaceholder,
    tracks = listOf(trackPlaceholder),
    intro = timeRangePlaceholder,
    outro = timeRangePlaceholder,
    server = "hd-1"
)

val episodeSourcesResponsePlaceholder = EpisodeSourcesResponse(
    streamingLink = episodeSourcesPlaceholder,
    servers = listOf(episodeServerPlaceholder)
)
