package com.example.animeapp.data.local.entities

import androidx.room.TypeConverter
import com.example.animeapp.models.*
import kotlinx.serialization.json.Json

class AnimeDetailComplementConverter {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromEpisodesList(episodes: List<Episode>): String {
        return json.encodeToString(episodes)
    }

    @TypeConverter
    fun toEpisodesList(episodesJson: String): List<Episode>? {
        return json.decodeFromString(episodesJson)
    }
}