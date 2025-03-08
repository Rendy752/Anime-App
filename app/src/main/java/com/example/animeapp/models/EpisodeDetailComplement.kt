package com.example.animeapp.models

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(
    tableName = "episode_detail_complement",
    primaryKeys = ["id"]
)

@Parcelize
@Serializable
data class EpisodeDetailComplement(
    val id: String,
    val servers: EpisodeServersResponse,
    val sources: EpisodeSourcesResponse,
    val is_favorite: Boolean = false,
    val is_watched: Boolean = false,
    val last_watched: String? = null,
    val last_timestamp: Long? = null,
) : Parcelable