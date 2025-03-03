package com.example.animeapp.utils

import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.repository.AnimeStreamingRepository

object StreamingUtils {
    suspend fun getDefaultEpisodeSources(response: Resource<EpisodeServersResponse>, animeStreamingRepository: AnimeStreamingRepository): Resource<EpisodeSourcesResponse> {
        val episodeServers = response.data
        val episodeDefaultId = episodeServers?.episodeId
            ?: return Resource.Error("No default episode found")

        val episodeSourcesQuery = episodeServers.let {
            when {
                it.sub.isNotEmpty() -> EpisodeSourcesQuery(
                    episodeDefaultId,
                    it.sub.first().serverName,
                    "sub"
                )

                it.dub.isNotEmpty() -> EpisodeSourcesQuery(
                    episodeDefaultId,
                    it.dub.first().serverName,
                    "dub"
                )

                it.raw.isNotEmpty() -> EpisodeSourcesQuery(
                    episodeDefaultId,
                    it.raw.first().serverName,
                    "raw"
                )

                else -> null
            }
        } ?: return Resource.Error("No episode servers found")

        return try {
            ResponseHandler.handleCommonResponse(
                animeStreamingRepository.getEpisodeSources(
                    episodeSourcesQuery.id,
                    episodeSourcesQuery.server,
                    episodeSourcesQuery.category
                )
            )
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }
}