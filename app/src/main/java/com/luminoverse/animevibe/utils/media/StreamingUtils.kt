package com.luminoverse.animevibe.utils.media

import android.util.Log
import com.luminoverse.animevibe.models.EpisodeServersResponse
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.EpisodeSourcesResponse
import com.luminoverse.animevibe.utils.resource.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object StreamingUtils {
    val failedServers = mutableMapOf<String, Long>()
    private const val FAILURE_CACHE_DURATION_MS = 30 * 60 * 1000L

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

        val usedServers = mutableSetOf<String>()

        for (query in queriesToTry) {
            val serverKey = "${query.server}-${query.category}"
            if (serverKey in usedServers) {
                continue
            }
            usedServers.add(serverKey)

            try {
                val sourcesResult = getEpisodeSources(query.id, query.server, query.category)
                if (sourcesResult is Resource.Success && sourcesResult.data.sources.isNotEmpty()) {
                    val sourceUrl = sourcesResult.data.sources[0].url
                    if (isServerAvailable(sourceUrl)) {
                        return Pair(sourcesResult, query)
                    } else {
                        markServerFailed(query.server, query.category)
                        Log.w(
                            "StreamingUtils",
                            "Server ${query.server} (${query.category}) URL unavailable: $sourceUrl"
                        )
                    }
                } else {
                    markServerFailed(query.server, query.category)
                }
            } catch (e: Exception) {
                markServerFailed(query.server, query.category)
                Log.e(
                    "StreamingUtils",
                    "Failed to fetch sources for server ${query.server} (${query.category})",
                    e
                )
            }
        }

        return Pair(
            Resource.Error("All available servers failed to provide episode sources"),
            null
        )
    }

    suspend fun isServerAvailable(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e("StreamingUtils", "Server availability check failed for $url", e)
            false
        }
    }

    fun markServerFailed(server: String, category: String) {
        val key = "$server-$category"
        failedServers[key] = System.currentTimeMillis()
    }

    fun isServerRecentlyFailed(server: String, category: String): Boolean {
        val key = "$server-$category"
        val failureTime = failedServers[key] ?: return false
        val isExpired = System.currentTimeMillis() - failureTime > FAILURE_CACHE_DURATION_MS
        if (isExpired) {
            failedServers.remove(key)
            return false
        }
        return true
    }
}