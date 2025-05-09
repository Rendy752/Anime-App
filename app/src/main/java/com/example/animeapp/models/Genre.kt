package com.example.animeapp.models

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(
    tableName = "genre",
    primaryKeys = ["mal_id"]
)

@Serializable
@Parcelize
data class Genre(
    val mal_id: Int,
    val name: String,
    val url: String,
    val count: Int
) : Parcelable

val genrePlaceholder = Genre(mal_id = 1, name = "Action", url = "", count = 0)