package com.example.animeapp.dataclass

data class AnimeRecommendation(
val mal_id: String,
val entry: List<AnimeHeader>,
val content: String,
val date: String,
val user: User
)
