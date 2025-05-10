package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class EpisodeServersResponse(
    val episodeId: String,
    val episodeNo: Int,
    val sub: List<Server>,
    val dub: List<Server>,
    val raw: List<Server>
): Parcelable

val episodeServersResponsePlaceholder = EpisodeServersResponse(
    episodeId = "lorem-ipsum-123?ep=123",
    episodeNo = 1,
    sub = listOf(serverPlaceholder),
    dub = listOf(serverPlaceholder),
    raw = listOf(serverPlaceholder)
)