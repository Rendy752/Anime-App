package com.example.animeapp.models

import androidx.room.Entity

@Entity(
    tableName = "anime_recommendations",
    primaryKeys = ["mal_id"]
)
data class AnimeRecommendation(
val mal_id: String,
val entry: List<AnimeHeader>,
val content: String,
val date: String,
val user: User
)
