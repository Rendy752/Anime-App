package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Producer(
    val mal_id: Int,
    val url: String?,
    val titles: List<Title>?,
    val images: ProducerImage?,
    val favorites: Int,
    val established: String?,
    val about: String?,
    val count: Int
): Parcelable

@Parcelize
@Serializable
data class ProducerImage(
    val jpg: JpgImage?
): Parcelable

@Parcelize
@Serializable
data class JpgImage(
    val image_url: String?
): Parcelable