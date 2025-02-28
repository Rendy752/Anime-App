package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeServersResponse(
    val episodeId: String,
    val episodeNo: Int,
    val sub: List<Server>,
    val dub: List<Server>,
    val raw: List<Server>
)