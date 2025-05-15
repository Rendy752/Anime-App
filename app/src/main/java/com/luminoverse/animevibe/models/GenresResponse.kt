package com.luminoverse.animevibe.models

import kotlinx.serialization.Serializable

@Serializable
data class GenresResponse(
    val data: List<Genre>,
)

