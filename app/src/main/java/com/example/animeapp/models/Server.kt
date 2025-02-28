package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Server(
    val serverName: String,
    val serverId: Int
)