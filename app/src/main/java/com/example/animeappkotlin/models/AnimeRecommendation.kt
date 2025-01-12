package com.example.animeappkotlin.models

import androidx.room.Entity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
