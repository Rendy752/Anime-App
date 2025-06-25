package com.luminoverse.animevibe.ui.common

import kotlinx.serialization.Serializable

@Serializable
data class AnimeAniwatchCommonResponse<T>(
    val success: Boolean,
    val results: T
)