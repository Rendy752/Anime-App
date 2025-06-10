package com.luminoverse.animevibe.data.local.entities

import androidx.room.TypeConverter
import com.luminoverse.animevibe.models.EpisodeServersResponse
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.EpisodeSourcesResponse
import kotlinx.serialization.json.Json

class EpisodeDetailComplementConverter {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromEpisodeServersResponse(value: EpisodeServersResponse): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toEpisodeServersResponse(value: String): EpisodeServersResponse {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromEpisodeSourcesResponse(value: EpisodeSourcesResponse): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toEpisodeSourcesResponse(value: String): EpisodeSourcesResponse {
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