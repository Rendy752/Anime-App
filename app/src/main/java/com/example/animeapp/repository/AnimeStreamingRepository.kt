package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI

class AnimeStreamingRepository(private val animeAPI: AnimeAPI) {
    suspend fun getAnimeAniwatchSearch(keyword: String) = animeAPI.getAnimeAniwatchSearch(keyword)

    suspend fun getEpisodes(id: String) = animeAPI.getEpisodes(id)

    suspend fun getEpisodeServers(episodeId: String) = animeAPI.getEpisodeServers(episodeId)

    suspend fun getEpisodeSources(episodeId: String, server: String, category: String) =
        animeAPI.getEpisodeSources(episodeId, server, category)
}