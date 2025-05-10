package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Server(
    val serverName: String,
    val serverId: Int
): Parcelable

val serverPlaceholder = Server(
    serverName = "vidstreaming",
    serverId = 1
)