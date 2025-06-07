package com.luminoverse.animevibe.utils.media

import android.util.Log
import com.luminoverse.animevibe.models.EpisodeServersResponse
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.EpisodeSourcesResponse
import com.luminoverse.animevibe.utils.resource.Resource

object StreamingUtils {
    fun getDefaultEpisodeQueries(
        response: Resource<EpisodeServersResponse>,
        episodeId: String
    ): List<EpisodeSourcesQuery> {
        val episodeServers = response.data ?: return emptyList()
        val queries = mutableListOf<EpisodeSourcesQuery>()

        episodeServers.sub.reversed().forEach { server ->
            queries.add(EpisodeSourcesQuery.create(episodeId, server.serverName, "sub"))
        }

        episodeServers.dub.reversed().forEach { server ->
            queries.add(EpisodeSourcesQuery.create(episodeId, server.serverName, "dub"))
        }

        episodeServers.raw.reversed().forEach { server ->
            queries.add(EpisodeSourcesQuery.create(episodeId, server.serverName, "raw"))
        }

        return queries
    }

    suspend fun getEpisodeSources(
        response: Resource<EpisodeServersResponse>,
        getEpisodeSources: suspend (String, String, String) -> Resource<EpisodeSourcesResponse>,
        errorSourceQueryList: List<EpisodeSourcesQuery> = emptyList(),
        episodeSourcesQuery: EpisodeSourcesQuery? = null
    ): Pair<Resource<EpisodeSourcesResponse>, EpisodeSourcesQuery?> {
        val episodeDefaultId = response.data?.episodeId
            ?: return Pair(Resource.Error("No default episode found"), null)

        val defaultQueries = getDefaultEpisodeQueries(response, episodeDefaultId)
        if (defaultQueries.isEmpty()) {
            return Pair(Resource.Error("No episode servers found"), null)
        }

        val prioritizedQueries = mutableListOf<EpisodeSourcesQuery>()

        episodeSourcesQuery?.let {
            if (!errorSourceQueryList.contains(it)) {
                prioritizedQueries.add(it)
            }
        }
        prioritizedQueries.addAll(defaultQueries.filter {
            !errorSourceQueryList.contains(it) && !prioritizedQueries.contains(
                it
            )
        })

        for (query in prioritizedQueries) {
            try {
                val sourcesResult = getEpisodeSources(query.id, query.server, query.category)
                if (sourcesResult is Resource.Success && sourcesResult.data.sources.isNotEmpty()) {
                    return Pair(sourcesResult, query)
                }
            } catch (e: Exception) {
                Log.e(
                    "StreamingUtils",
                    "Failed to fetch sources for server ${query.server} (${query.category})", e
                )
            }
        }

        return Pair(
            Resource.Error("All available servers failed to provide episode sources"),
            null
        )
    }
}