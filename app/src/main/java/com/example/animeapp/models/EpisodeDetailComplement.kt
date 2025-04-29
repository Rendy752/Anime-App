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
    val number: Int,
    val isFiller: Boolean,
    val servers: EpisodeServersResponse,
    val sources: EpisodeSourcesResponse,
    val sourcesQuery: EpisodeSourcesQuery,
    val isFavorite: Boolean = false,
    val lastWatched: String? = null,
    val lastTimestamp: Long? = null,
    val createdAt: Long = Instant.now().epochSecond,
    var updatedAt: Long = Instant.now().epochSecond
) : Parcelable

val episodeDetailComplementPlaceholder = EpisodeDetailComplement(
    id = "naruto-shippuden-355?ep=8203",
    malId = animeDetailPlaceholder.mal_id,
    aniwatchId = "naruto-shippuden-355",
    animeTitle = animeDetailPlaceholder.title,
    episodeTitle = "Madara Uchiha",
    number = 322,
    isFiller = false,
    imageUrl = animeDetailPlaceholder.images.webp.large_image_url,
    servers = episodeServersResponsePlaceholder,
    sources = episodeSourcesResponsePlaceholder,
    sourcesQuery = episodeSourcesQueryPlaceholder,
    isFavorite = false,
    lastWatched = null,
    lastTimestamp = null,
)