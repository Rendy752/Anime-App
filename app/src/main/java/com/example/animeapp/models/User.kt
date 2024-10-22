package com.example.animeappkotlin.models

data class User(
    val url: String,
    val username: String
) {
    constructor(url: String, username: String, s: String) : this(url, username)
}
