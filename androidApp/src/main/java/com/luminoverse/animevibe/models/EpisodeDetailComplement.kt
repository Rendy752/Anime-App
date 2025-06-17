package com.luminoverse.animevibe.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
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
@Stable
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
    val servers: List<EpisodeServer>,
    val sources: EpisodeSources,
    val sourcesQuery: EpisodeSourcesQuery,
    val isFavorite: Boolean = false,
    val lastWatched: String? = null,
    val lastTimestamp: Long? = null,
    val duration: Long? = null,
    val createdAt: Long = Instant.now().epochSecond,
    val updatedAt: Long = Instant.now().epochSecond
) : Parcelable

val episodeDetailComplementPlaceholder = EpisodeDetailComplement(
    id = "lorem-ipsum-123?ep=123",
    malId = 1735,
    aniwatchId = "anime-1735",
    animeTitle = "Naruto: Shippuuden",
    episodeTitle = "Title of Episode",
    imageUrl = "https://cdn.myanimelist.net/images/anime/1565/111305l.webp",
    screenshot = null,
    number = 1,
    isFiller = false,
    servers = listOf(episodeServerPlaceholder),
    sources = episodeSourcesPlaceholder,
    sourcesQuery = episodeSourcesQueryPlaceholder,
    isFavorite = false,
    lastWatched = null,
    lastTimestamp = null,
    duration = null,
    createdAt = 1746872830,
    updatedAt = 1746872830
)