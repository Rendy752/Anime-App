package com.luminoverse.animevibe.models

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Entity(
    tableName = "anime_recommendations",
    primaryKeys = ["mal_id"]
)

@Serializable
data class AnimeRecommendation(
    val mal_id: String,
    val entry: List<AnimeHeader>,
    val content: String,
    val date: String,
    val user: User
)

val animeRecommendationPlaceholder = AnimeRecommendation(
    mal_id = "4654-52991",
    entry = listOf(
        AnimeHeader(
            mal_id = 4654,
            url = "https://myanimelist.net/anime/4654/Toaru_Majutsu_no_Index",
            images = Images(
                jpg = ImageUrl(
                    image_url = "https://cdn.myanimelist.net/images/anime/2/75533.jpg",
                    small_image_url = "https://cdn.myanimelist.net/images/anime/2/75533t.jpg",
                    medium_image_url = "https://cdn.myanimelist.net/images/anime/2/75533m.jpg",
                    large_image_url = "https://cdn.myanimelist.net/images/anime/2/75533l.jpg",
                    maximum_image_url = "https://cdn.myanimelist.net/images/anime/2/75533.jpg"
                ),
                webp = ImageUrl(
                    image_url = "https://cdn.myanimelist.net/images/anime/2/75533.webp",
                    small_image_url = "https://cdn.myanimelist.net/images/anime/2/75533t.webp",
                    medium_image_url = "https://cdn.myanimelist.net/images/anime/2/75533m.webp",
                    large_image_url = "https://cdn.myanimelist.net/images/anime/2/75533l.webp",
                    maximum_image_url = "https://cdn.myanimelist.net/images/anime/2/75533.webp"
                )
            ),
            title = "Toaru Majutsu no Index"
        ),
        AnimeHeader(
            mal_id = 52991,
            url = "https://myanimelist.net/anime/52991/Sousou_no_Frieren",
            images = Images(
                jpg = ImageUrl(
                    image_url = "https://cdn.myanimelist.net/images/anime/1015/138006.jpg",
                    small_image_url = "https://cdn.myanimelist.net/images/anime/1015/138006t.jpg",
                    medium_image_url = "https://cdn.myanimelist.net/images/anime/1015/138006m.jpg",
                    large_image_url = "https://cdn.myanimelist.net/images/anime/1015/138006l.jpg",
                    maximum_image_url = "https://cdn.myanimelist.net/images/anime/1015/138006.jpg"
                ),
                webp = ImageUrl(
                    image_url = "https://cdn.myanimelist.net/images/anime/1015/138006.webp",
                    small_image_url = "https://cdn.myanimelist.net/images/anime/1015/138006t.webp",
                    medium_image_url = "https://cdn.myanimelist.net/images/anime/1015/138006m.webp",
                    large_image_url = "https://cdn.myanimelist.net/images/anime/1015/138006l.webp",
                    maximum_image_url = "https://cdn.myanimelist.net/images/anime/1015/138006.webp"
                )
            ),
            title = "Sousou no Frieren"
        )
    ),
    content = "This recommendation encompasses the entirety of the Toaru Universe (ie; Railgun, Accelerator, ITEM, etc). Both media perfectly illustrates the unique applications of power & magic within its unique power systems; that is also blended with a unique world that has their own deep history which enhances their world building. The intricacies of the supernatural/magical abilities gets delved-in deeply and yet maintains coherence that grounds our understanding. Furthermore, their applications can cohesively work together with other character's abilities as well which adds such originality and cleverness to the overall storytelling.",
    date = "2025-03-16T11:22:39+00:00",
    user = User(url = "https://myanimelist.net/profile/IchiroEX", username = "IchiroEX")
)
