package com.luminoverse.animevibe.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Stable
data class Server(
    val serverName: String,
    val serverId: Int
): Parcelable

val serverPlaceholder = Server(
    serverName = "vidstreaming",
    serverId = 1
)