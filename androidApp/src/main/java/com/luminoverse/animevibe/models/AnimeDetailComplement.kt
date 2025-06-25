package com.luminoverse.animevibe.models

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.Instant

@Entity(
    tableName = "anime_detail_complement",
    primaryKeys = ["id"]
)

@Parcelize
@Serializable
data class AnimeDetailComplement(
    val id: String,
    val malId: Int,
    val isFavorite: Boolean = false,
    val eps: Int? = null,
    val sub: Int? = null,
    val dub: Int? = null,
    val lastEpisodeWatchedId: String? = null,
    val episodes: List<Episode>? = null,
    val lastEpisodeUpdatedAt: Long = Instant.now().epochSecond,
    val createdAt: Long = Instant.now().epochSecond,
    val updatedAt: Long = Instant.now().epochSecond
) : Parcelable

val animeDetailComplementPlaceholder = AnimeDetailComplement(
    id = "anime-1735",
    malId = 1735,
    isFavorite = false,
    eps = 1,
    sub = 0,
    dub = 0,
    lastEpisodeWatchedId = null,
    episodes = listOf(episodePlaceholder),
    lastEpisodeUpdatedAt = 1746872831,
    createdAt = 1746872831,
    updatedAt = 1746872831
)