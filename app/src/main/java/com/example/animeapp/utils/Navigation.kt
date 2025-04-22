package com.example.animeapp.utils

import androidx.navigation.NavController
import com.example.animeapp.models.CommonIdentity
import com.google.gson.Gson
import java.net.URLEncoder

object Navigation {
    fun NavController.navigateToAnimeWatch(
        malId: Int,
        episodeId: String,
    ) {
        val malIdEncoded = URLEncoder.encode(malId.toString(), "UTF-8")
        val episodeIdEncoded = URLEncoder.encode(episodeId, "UTF-8")

        navigate("animeWatch/$malIdEncoded/$episodeIdEncoded")
    }

    fun NavController.navigateWithFilter(
        data: CommonIdentity?,
        genreFilter: Boolean = false
    ) {
        val gson = Gson()
        val commonIdentityString = data?.let { URLEncoder.encode(gson.toJson(it), "UTF-8") }

        val genrePart = if (genreFilter) commonIdentityString ?: "null" else "null"
        val producerPart = if (!genreFilter) commonIdentityString ?: "null" else "null"

        navigate("search/$genrePart/$producerPart")
    }

    fun NavController.navigateToAnimeDetail(id: Int) {
        navigate("animeDetail/$id")
    }
}