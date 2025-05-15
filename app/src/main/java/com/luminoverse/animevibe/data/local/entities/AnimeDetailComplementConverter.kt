package com.luminoverse.animevibe.data.local.entities

import androidx.room.TypeConverter
import com.luminoverse.animevibe.models.*
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