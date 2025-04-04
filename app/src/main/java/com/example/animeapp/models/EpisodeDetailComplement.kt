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
    val title: String,
    val imageUrl: String?,
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
    id = "watch-1",
    title = "Title of Episode",
    imageUrl = "https://cdn.myanimelist.net/images/",
    servers = episodeServersResponsePlaceholder,
    sources = episodeSourcesResponsePlaceholder,
    sourcesQuery = episodeSourcesQueryPlaceholder,
    isFavorite = false,
    lastWatched = null,
    lastTimestamp = null,
)