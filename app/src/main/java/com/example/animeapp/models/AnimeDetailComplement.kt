package com.example.animeapp.models

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(
    tableName = "anime_detail_complement",
    primaryKeys = ["id"]
)

@Parcelize
@Serializable
data class AnimeDetailComplement(
    val id: String,
    val mal_id: Int,
    val is_favorite: Boolean,
    val eps: Int? = null,
    val sub: Int? = null,
    val dub: Int? = null,
    val last_episode_watched: Int? = null,
    val episodes: List<Episode>
): Parcelable