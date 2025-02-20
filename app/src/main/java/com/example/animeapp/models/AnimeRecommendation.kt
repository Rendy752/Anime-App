package com.example.animeapp.models

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
