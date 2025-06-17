package com.luminoverse.animevibe.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Stable
data class EpisodeSourcesQuery(
    val id: String,
    val server: String,
    val category: String
) : Parcelable {

    companion object {
        private val serverMap = mapOf(
            "vidsrc" to "vidstreaming"
        )

        fun create(id: String, rawServer: String, category: String): EpisodeSourcesQuery {
            val mappedServer = serverMap.getOrDefault(rawServer, rawServer)
            return EpisodeSourcesQuery(id = id, server = mappedServer, category = category)
        }
    }
}

val episodeSourcesQueryPlaceholder = EpisodeSourcesQuery(
    id = "lorem-ipsum-123?ep=123",
    server = "HD-2",
    category = "sub"
)