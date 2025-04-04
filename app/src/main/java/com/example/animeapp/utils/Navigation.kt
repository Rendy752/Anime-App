package com.example.animeapp.utils

import androidx.navigation.NavController
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.CommonIdentity
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.google.gson.Gson
import java.net.URLEncoder

object Navigation {
    fun NavController.navigateToAnimeWatch(
        animeDetail: AnimeDetail,
        animeDetailComplement: AnimeDetailComplement,
        episodeId: String,
        episodes: List<Episode>,
        defaultEpisode: EpisodeDetailComplement
    ) {
        val gson = Gson()
        val animeDetailJson = URLEncoder.encode(gson.toJson(animeDetail), "UTF-8")
        val animeDetailComplementJson =  URLEncoder.encode(gson.toJson(animeDetailComplement), "UTF-8")
        val episodeIdEncoded = URLEncoder.encode(episodeId, "UTF-8")
        val episodesJson = URLEncoder.encode(gson.toJson(episodes), "UTF-8")
        val defaultEpisodeJson = URLEncoder.encode(gson.toJson(defaultEpisode), "UTF-8")

        navigate("animeWatch/$animeDetailJson/$animeDetailComplementJson/$episodeIdEncoded/$episodesJson/$defaultEpisodeJson")
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
}