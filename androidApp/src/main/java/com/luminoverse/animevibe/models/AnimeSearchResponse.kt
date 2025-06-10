package com.luminoverse.animevibe.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeSearchResponse(
    val data: List<AnimeDetail>,
    val pagination: CompletePagination
)