package com.luminoverse.animevibe.utils.media

import android.util.Log
import com.luminoverse.animevibe.models.EpisodeServer
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.EpisodeSourcesResponse
import com.luminoverse.animevibe.ui.common.AnimeAniwatchCommonResponse
import com.luminoverse.animevibe.utils.resource.Resource

/**
 * Utility object for handling streaming-related operations.
 */
object StreamingUtils {
    /**
     * Generates a list of default [EpisodeSourcesQuery] objects from a list of [EpisodeServer] objects.
     * @param episodeId The ID of the episode.
     * @param episodeServers A list of [EpisodeServer] objects.
     * @return A list of [EpisodeSourcesQuery] objects.
     */
    private fun getDefaultEpisodeQueries(
        episodeId: String,
        episodeServers: List<EpisodeServer>
    ): List<EpisodeSourcesQuery> {
        return episodeServers.map { episodeServer ->
            EpisodeSourcesQuery.create(
                episodeId, episodeServer.serverName, episodeServer.type
            )
        }
    }

    /**
     * Retrieves episode sources by trying different servers and categories.
     *
     * This function attempts to fetch episode sources using a prioritized list of queries.
     * It starts with a provided [episodeSourcesQuery] (if any and not in the error list),
     * then tries up to 3 default queries, excluding any that are in the [errorSourceQueryList]
     * or already prioritized.
     *
     * @param episodeId The ID of the episode.
     * @param episodeServers A list of available [EpisodeServer] objects.
     * @param getEpisodeSources A suspend function that fetches episode sources given an episode ID, server name, and category.
     * @param emptySourcesCheck A boolean flag indicating whether to check if the sources are empty (default is true).
     * @param errorSourceQueryList A list of [EpisodeSourcesQuery] that have previously resulted in errors.
     * @param episodeSourcesQuery An optional [EpisodeSourcesQuery] to prioritize.
     * @return A [Pair] containing the [Resource] of the fetched episode sources and the successful [EpisodeSourcesQuery], or null if all attempts fail.
     */
    suspend fun getEpisodeSourcesResult(
        episodeId: String,
        episodeServers: List<EpisodeServer>,
        getEpisodeSources: suspend (String, String, String) -> Resource<AnimeAniwatchCommonResponse<EpisodeSourcesResponse>>,
        emptySourcesCheck: Boolean = true,
        errorSourceQueryList: List<EpisodeSourcesQuery> = emptyList(),
        episodeSourcesQuery: EpisodeSourcesQuery? = null
    ): Pair<Resource<AnimeAniwatchCommonResponse<EpisodeSourcesResponse>>, EpisodeSourcesQuery?> {
        val defaultQueries = getDefaultEpisodeQueries(episodeId, episodeServers)
        if (defaultQueries.isEmpty()) {
            return Pair(Resource.Error("No episode servers found"), null)
        }

        val prioritizedQueries = mutableListOf<EpisodeSourcesQuery>()

        episodeSourcesQuery?.let {
            if (!errorSourceQueryList.contains(it)) {
                prioritizedQueries.add(it)
            }
        }
        prioritizedQueries.addAll(defaultQueries.take(3).filter {
            !errorSourceQueryList.contains(it) && !prioritizedQueries.contains(it)
        })

        for (query in prioritizedQueries) {
            try {
                val sourcesResult = getEpisodeSources(query.id, query.server, query.category)
                val isSuccess = sourcesResult is Resource.Success
                val hasSources = if (emptySourcesCheck) {
                    sourcesResult is Resource.Success && sourcesResult.data.results.streamingLink.link.file.isNotEmpty()
                } else isSuccess
                if (isSuccess && hasSources) {
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