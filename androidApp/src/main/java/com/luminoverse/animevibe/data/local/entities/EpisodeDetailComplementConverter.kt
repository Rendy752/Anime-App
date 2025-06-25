package com.luminoverse.animevibe.data.local.entities

import androidx.room.TypeConverter
import com.luminoverse.animevibe.models.EpisodeServer
import com.luminoverse.animevibe.models.EpisodeSources
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import kotlinx.serialization.json.Json

class EpisodeDetailComplementConverter {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromEpisodeServerList(value: List<EpisodeServer>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toEpisodeServerList(value: String): List<EpisodeServer> {
        return json.decodeFromString<List<EpisodeServer>>(value)
    }

    @TypeConverter
    fun fromEpisodeSources(value: EpisodeSources): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toEpisodeSources(value: String): EpisodeSources {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromEpisodeSourcesQuery(value: EpisodeSourcesQuery): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toEpisodeSourcesQuery(value: String): EpisodeSourcesQuery {
        return json.decodeFromString(value)
    }
}