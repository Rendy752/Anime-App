package com.example.animeapp.utils

import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.models.EpisodeSourcesResponse
import retrofit2.Response

object StreamingUtils {
    fun getEpisodeQuery(
        response: Resource<EpisodeServersResponse>,
        episodeId: String
    ): EpisodeSourcesQuery? {
        val episodeServers = response.data ?: return null
        return episodeServers.let {
            when {
                it.sub.isNotEmpty() -> EpisodeSourcesQuery(
                    episodeId,
                    it.sub.first().serverName,
                    "sub"
                )

                it.dub.isNotEmpty() -> EpisodeSourcesQuery(
                    episodeId,
                    it.dub.first().serverName,
                    "dub"
                )

                it.raw.isNotEmpty() -> EpisodeSourcesQuery(
                    episodeId,
                    it.raw.first().serverName,
                    "raw"
                )

                else -> null
            }
        }
    }

    suspend fun getEpisodeSources(
        response: Resource<EpisodeServersResponse>,
        getEpisodeSources: suspend (String, String, String) -> Response<EpisodeSourcesResponse>,
        episodeSourcesQuery: EpisodeSourcesQuery? = null
    ): Resource<EpisodeSourcesResponse> {
        val episodeServers = response.data
        val episodeDefaultId = episodeServers?.episodeId
            ?: return Resource.Error("No default episode found")

        val episodeQuery = episodeSourcesQuery ?: getEpisodeQuery(response, episodeDefaultId)
        ?: return Resource.Error("No episode servers found")

        return try {
            ResponseHandler.handleCommonResponse(
                getEpisodeSources(
                    episodeQuery.id,
                    episodeQuery.server,
                    episodeQuery.category
                )
            )
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }
}