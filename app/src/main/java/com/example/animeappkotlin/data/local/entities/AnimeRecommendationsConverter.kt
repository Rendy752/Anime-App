package com.example.animeappkotlin.data.local.entities

import androidx.room.TypeConverter
import com.example.animeappkotlin.models.AnimeHeader
import com.example.animeappkotlin.models.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AnimeRecommendationsConverter {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromAnimeHeaderList(value: List<AnimeHeader>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toAnimeHeaderList(value: String): List<AnimeHeader> {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromUser(value: User): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toUser(value: String): User {
        return json.decodeFromString(value)
    }
}