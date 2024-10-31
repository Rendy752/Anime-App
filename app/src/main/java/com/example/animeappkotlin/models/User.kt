package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val url: String,
    val username: String
) {
    constructor(url: String, username: String, s: String) : this(url, username)
}
