package com.example.animeapp.models

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.Instant

@Entity(
    tableName = "episode_detail_complement",
    primaryKeys = ["id"]
)

@Parcelize
@Serializable
data class EpisodeDetailComplement(
    val id: String,
    val malId: Int,
    val aniwatchId: String,
    val animeTitle: String,
    val episodeTitle: String,
    val imageUrl: String?,
    val screenshot: String? = null,
    val number: Int,
    val isFiller: Boolean,
    val servers: EpisodeServersResponse,
    val sources: EpisodeSourcesResponse,
    val sourcesQuery: EpisodeSourcesQuery,
    val isFavorite: Boolean = false,
    val lastWatched: String? = null,
    val lastTimestamp: Long? = null,
    val duration: Long? = null,
    val createdAt: Long = Instant.now().epochSecond,
    var updatedAt: Long = Instant.now().epochSecond
) : Parcelable

val episodeDetailComplementPlaceholder = EpisodeDetailComplement(
    id = "lorem-ipsum-123?ep=123",
    malId = 123,
    aniwatchId = "lorem-ipsum-123",
    animeTitle = "Anime Title",
    episodeTitle = "Episode Title",
    number = 123,
    isFiller = false,
    imageUrl = null,
    servers = episodeServersResponsePlaceholder,
    sources = episodeSourcesResponsePlaceholder,
    sourcesQuery = episodeSourcesQueryPlaceholder,
    isFavorite = false,
    lastWatched = null,
    lastTimestamp = null,
)