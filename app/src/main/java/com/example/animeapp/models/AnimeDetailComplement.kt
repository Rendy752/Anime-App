package com.example.animeapp.models

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
    val episodes: List<Episode>,
    val lastEpisodeUpdatedAt: Long = Instant.now().epochSecond,
    val createdAt: Long = Instant.now().epochSecond,
    var updatedAt: Long = Instant.now().epochSecond
) : Parcelable

val animeDetailComplementPlaceholder = AnimeDetailComplement(
    id = "",
    malId = 0,
    isFavorite = false,
    eps = 0,
    sub = 0,
    dub = 0,
    lastEpisodeWatchedId = null,
    episodes = listOf(episodePlaceholder),
    lastEpisodeUpdatedAt = Instant.now().epochSecond,
    createdAt = Instant.now().epochSecond,
    updatedAt = Instant.now().epochSecond
)