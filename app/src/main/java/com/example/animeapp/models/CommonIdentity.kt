package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class CommonIdentity(
    val mal_id: Int,
    val type: String,
    val name: String,
    val url: String
) : Parcelable {
    fun mapToGenre(): Genre {
        return Genre(mal_id, name, url, 0)
    }

    fun mapToProducer(): Producer {
        return Producer(mal_id, url, listOf(Title(type, name)), null, 0, null, null, 0)
    }
}