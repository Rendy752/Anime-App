package com.luminoverse.animevibe.utils.media

import com.luminoverse.animevibe.models.EpisodeServersResponse
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.EpisodeSourcesResponse
import com.luminoverse.animevibe.utils.resource.Resource

object StreamingUtils {
    private fun getDefaultEpisodeQueries(
        response: Resource<EpisodeServersResponse>,
        episodeId: String
    ): List<EpisodeSourcesQuery> {
        val episodeServers = response.data ?: return emptyList()
        val queries = mutableListOf<EpisodeSourcesQuery>()

        episodeServers.sub.reversed().forEach { server ->
            queries.add(EpisodeSourcesQuery(episodeId, server.serverName, "sub"))
        }

        episodeServers.dub.reversed().forEach { server ->
            queries.add(EpisodeSourcesQuery(episodeId, server.serverName, "dub"))
        }

        episodeServers.raw.reversed().forEach { server ->
            queries.add(EpisodeSourcesQuery(episodeId, server.serverName, "raw"))
        }

        return queries
    }

    suspend fun getEpisodeSources(
        response: Resource<EpisodeServersResponse>,
        getEpisodeSources: suspend (String, String, String) -> Resource<EpisodeSourcesResponse>,
        episodeSourcesQuery: EpisodeSourcesQuery? = null
    ): Pair<Resource<EpisodeSourcesResponse>, EpisodeSourcesQuery?> {
        val episodeDefaultId = response.data?.episodeId
            ?: return Pair(Resource.Error("No default episode found"), null)

        val allQueries = getDefaultEpisodeQueries(response, episodeDefaultId)
        if (allQueries.isEmpty()) {
            return Pair(Resource.Error("No episode servers found"), null)
        }

        val queriesToTry = mutableListOf<EpisodeSourcesQuery>()
        if (episodeSourcesQuery != null) {
            queriesToTry.add(episodeSourcesQuery)
        }
        queriesToTry.addAll(allQueries)

        val usedServers = mutableSetOf<String>()

        for (query in queriesToTry) {
            val serverKey = "${query.server}-${query.category}"
            if (serverKey in usedServers) {
                continue
            }
            usedServers.add(serverKey)

            try {
                val result = getEpisodeSources(
                    query.id,
                    query.server,
                    query.category
                )
                if (result is Resource.Success) {
                    return Pair(result, query)
                }
            } catch (e: Exception) {
                println("Failed to fetch sources for server ${query.server} (${query.category}): ${e.message}")
            }
        }

        return Pair(
            Resource.Error("All available servers failed to provide episode sources"),
            null
        )
    }
}