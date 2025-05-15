package com.luminoverse.animevibe.models

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class EpisodeSourcesQuery(
    val id: String,
    var server: String,
    val category: String
) : Parcelable {
    @IgnoredOnParcel
    private val serverMap = mapOf(
        "vidsrc" to "vidstreaming"
    )

    init {
        this.server = serverMap.getOrDefault(server, server)
    }
}

val episodeSourcesQueryPlaceholder = EpisodeSourcesQuery(
    id = "lorem-ipsum-123?ep=123",
    server = "vidstreaming",
    category = "sub"
)