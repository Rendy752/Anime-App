package com.luminoverse.animevibe.utils.media

import android.util.Log
import com.luminoverse.animevibe.models.EpisodeServersResponse
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.EpisodeSourcesResponse
import com.luminoverse.animevibe.utils.resource.Resource

object StreamingUtils {
    private fun getDefaultEpisodeQueries(episodeServersResponse: EpisodeServersResponse): List<EpisodeSourcesQuery> {
        return mapOf(
            "sub" to episodeServersResponse.sub,
            "dub" to episodeServersResponse.dub,
            "raw" to episodeServersResponse.raw
        ).flatMap { (category, servers) ->
            servers.reversed().map { server ->
                EpisodeSourcesQuery.create(
                    episodeServersResponse.episodeId, server.serverName, category
                )
            }
        }
    }

    suspend fun getEpisodeSourcesResult(
        episodeServersResponse: EpisodeServersResponse,
        getEpisodeSources: suspend (String, String, String) -> Resource<EpisodeSourcesResponse>,
        errorSourceQueryList: List<EpisodeSourcesQuery> = emptyList(),
        episodeSourcesQuery: EpisodeSourcesQuery? = null
    ): Pair<Resource<EpisodeSourcesResponse>, EpisodeSourcesQuery?> {
        val defaultQueries = getDefaultEpisodeQueries(episodeServersResponse)
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
            !errorSourceQueryList.contains(it) && !prioritizedQueries.contains(it)
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