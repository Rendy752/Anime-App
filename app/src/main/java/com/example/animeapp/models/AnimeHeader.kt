package com.example.animeapp.models

data class AnimeHeader(
    val mal_id: Int,
    val url: String,
    val images: Images,
    val title: String
) {
    constructor(mal_id: Int, url: String) : this(
        mal_id,
        url,
        Images(ImageUrl("", "", "", "", ""), ImageUrl("", "", "", "", "")),
        ""
    )
}
