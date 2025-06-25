package com.luminoverse.animevibe.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Stable
data class EpisodeServer(
    val type: String,
    val data_id: String,
    val server_id: String,
    val serverName: String
) : Parcelable

val episodeServerPlaceholder = EpisodeServer(
    type = "dub",
    data_id = "1159073",
    server_id = "1",
    serverName = "HD-2"
)