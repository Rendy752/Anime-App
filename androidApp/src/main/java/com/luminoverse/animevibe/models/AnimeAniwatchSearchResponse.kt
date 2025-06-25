package com.luminoverse.animevibe.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeAniwatchSearchResponse(
    val data: List<AnimeAniwatch>,
    val totalPage: Int,
)

@Serializable
data class AnimeAniwatch(
    val id: String,
    val malID: Int,
    val title: String,
    val japanese_title: String,
    val poster: String,
    val duration: String,
    val tvInfo: TvInfo
)

@Serializable
data class TvInfo(
    val showType: String,
    val rating: String? = null,
    val sub: Int? = null,
    val dub: Int? = null,
    val eps: Int? = null,
)

val animeAniwatchPlaceholder = AnimeAniwatch(
    id = "anime-1735",
    malID = 1735,
    title = "Naruto: Shippuuden",
    japanese_title = "Naruto: Shippuuden",
    poster = "",
    duration = "",
    tvInfo = TvInfo(
        showType = "",
        rating = "",
        sub = 0,
        dub = 0,
        eps = 0
    )
)