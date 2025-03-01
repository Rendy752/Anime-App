package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeSourcesQuery(
    val id: String,
    var server: String,
    val category: String
) {
    private val serverMap = mapOf(
        "vidsrc" to "vidstreaming"
    )

    init {
        this.server = serverMap.getOrDefault(server, server)
    }
}