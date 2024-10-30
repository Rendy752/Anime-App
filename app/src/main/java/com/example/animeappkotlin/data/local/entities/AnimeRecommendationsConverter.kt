package com.example.animeappkotlin.data.local.entities

import androidx.room.TypeConverter
import com.example.animeappkotlin.models.AnimeHeader
import com.example.animeappkotlin.models.User

class AnimeRecommendationsConverter {
    @TypeConverter
    fun fromAnimeHeaderList(value: List<AnimeHeader>): String {
        return value.joinToString(",") { it.mal_id.toString() }
    }

    @TypeConverter
    fun toAnimeHeaderList(value: String): List<AnimeHeader> {
        val malIds = value.split(",")
        return malIds.map { AnimeHeader(it.toInt(), "") }
    }

    @TypeConverter
    fun fromUser(value: User): String {
        return value.username
    }

    @TypeConverter
    fun toUser(value: String): User {
        return User(value, "", "")
    }
}