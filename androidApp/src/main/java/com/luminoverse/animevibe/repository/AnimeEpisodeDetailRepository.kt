package com.luminoverse.animevibe.repository

import com.luminoverse.animevibe.data.local.dao.AnimeDetailDao
import com.luminoverse.animevibe.data.local.dao.AnimeDetailComplementDao
import com.luminoverse.animevibe.data.local.dao.EpisodeDetailComplementDao
import com.luminoverse.animevibe.data.remote.api.AnimeAPI
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.AnimeDetailComplement
import com.luminoverse.animevibe.models.AnimeDetailResponse
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeHistoryQueryState
import com.luminoverse.animevibe.models.EpisodeServer
import com.luminoverse.animevibe.models.EpisodeSourcesResponse
import com.luminoverse.animevibe.models.EpisodesResponse
import com.luminoverse.animevibe.ui.common.AnimeAniwatchCommonResponse
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.resource.ResponseHandler
import com.luminoverse.animevibe.utils.resource.ResponseHandler.safeApiCall
import com.luminoverse.animevibe.utils.TimeUtils
import java.time.Instant

class AnimeEpisodeDetailRepository(
    private val animeDetailDao: AnimeDetailDao,
    private val animeDetailComplementDao: AnimeDetailComplementDao,
    private val episodeDetailComplementDao: EpisodeDetailComplementDao,
    private val jikanAPI: AnimeAPI,
    private val runwayAPI: AnimeAPI
) {
    suspend fun getAnimeDetail(id: Int): Resource<AnimeDetailResponse> {
        val cached = getCachedAnimeDetailById(id)
        if (cached != null && !isDataNeedUpdate(cached)) {
            return Resource.Success(AnimeDetailResponse(cached))
        }

        val response = safeApiCall { jikanAPI.getAnimeDetail(id) }
        val resource = ResponseHandler.handleCommonResponse(response)
        if (resource is Resource.Success) {
            animeDetailDao.insertAnimeDetail(resource.data.data)
        }
        return resource
    }

    suspend fun getCachedAnimeDetailById(id: Int): AnimeDetail? =
        animeDetailDao.getAnimeDetailById(id)

    suspend fun deleteAnimeDetailById(id: Int) {
        animeDetailDao.deleteAnimeDetailById(id)
    }

    suspend fun isDataNeedUpdate(
        animeDetail: AnimeDetail,
        animeDetailComplement: AnimeDetailComplement? = null
    ): Boolean {
        val complement =
            animeDetailComplement ?: getCachedAnimeDetailComplementByMalId(animeDetail.mal_id)
        return animeDetail.airing && complement?.id?.isNotEmpty() == true && !complement.id.all { it.isDigit() } &&
                TimeUtils.isEpisodeAreUpToDate(
                    animeDetail.broadcast.time,
                    animeDetail.broadcast.timezone,
                    animeDetail.broadcast.day,
                    complement.lastEpisodeUpdatedAt
                )
    }

    suspend fun getCachedAnimeDetailComplementByMalId(malId: Int): AnimeDetailComplement? =
        animeDetailComplementDao.getAnimeDetailComplementByMalId(malId)

    suspend fun insertCachedAnimeDetailComplement(animeDetailComplement: AnimeDetailComplement) =
        animeDetailComplementDao.insertAnimeDetailComplement(animeDetailComplement)

    suspend fun updateCachedAnimeDetailComplement(updatedAnimeDetailComplement: AnimeDetailComplement) =
        animeDetailComplementDao.updateAnimeDetailComplement(
            updatedAnimeDetailComplement.copy(updatedAt = Instant.now().epochSecond)
        )

    suspend fun deleteAnimeDetailComplement(malId: Int): Boolean {
        val anime = getCachedAnimeDetailComplementByMalId(malId) ?: return false
        animeDetailComplementDao.deleteAnimeDetailComplement(anime)
        episodeDetailComplementDao.deleteEpisodeDetailComplementByMalId(malId)
        return true
    }

    suspend fun getCachedLatestWatchedEpisodeDetailComplement(): EpisodeDetailComplement? =
        episodeDetailComplementDao.getLatestWatchedEpisodeDetailComplement()

    suspend fun getCachedEpisodeDetailComplement(id: String): EpisodeDetailComplement? =
        episodeDetailComplementDao.getEpisodeDetailComplementById(id)

    suspend fun insertCachedEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement) =
        episodeDetailComplementDao.insertEpisodeDetailComplement(episodeDetailComplement)

    suspend fun updateEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement) =
        episodeDetailComplementDao.updateEpisodeDetailComplement(
            episodeDetailComplement.copy(updatedAt = Instant.now().epochSecond)
        )

    suspend fun deleteEpisodeDetailComplement(id: String): Boolean {
        val episode = getCachedEpisodeDetailComplement(id) ?: return false
        episodeDetailComplementDao.deleteEpisodeDetailComplement(episode)
        return true
    }

    suspend fun getAnimeAniwatchSearch(keyword: String) =
        ResponseHandler.handleCommonResponse(safeApiCall { runwayAPI.getAnimeAniwatchSearch(keyword) })

    suspend fun getEpisodeServers(id: String): Resource<AnimeAniwatchCommonResponse<List<EpisodeServer>>> {
        val animeId = id.split("?ep=").first()
        val episodeId = id.split("?ep=").last()
        return ResponseHandler.handleCommonResponse(safeApiCall { runwayAPI.getEpisodeServers(animeId,episodeId) })
    }

    suspend fun getEpisodeSources(
        episodeId: String,
        server: String,
        type: String
    ): Resource<AnimeAniwatchCommonResponse<EpisodeSourcesResponse>> =
        ResponseHandler.handleCommonResponse(safeApiCall {
            runwayAPI.getEpisodeSources(episodeId, server, type)
        })

    suspend fun getEpisodes(id: String): Resource<AnimeAniwatchCommonResponse<EpisodesResponse>> =
        ResponseHandler.handleCommonResponse(safeApiCall { runwayAPI.getEpisodes(id) })

    suspend fun getAllEpisodeHistory(queryState: EpisodeHistoryQueryState): Resource<List<EpisodeDetailComplement>> {
        return try {
            Resource.Success(
                episodeDetailComplementDao.getAllEpisodeHistory(
                    isFavorite = queryState.isFavorite,
                    sortBy = queryState.sortBy.name
                )
            )
        } catch (e: Exception) {
            Resource.Error("Failed to fetch all episode history: ${e.message}")
        }
    }

    suspend fun getRandomCachedUnfinishedEpisode(): Pair<EpisodeDetailComplement?, Int> {
        val animeDetailsWithWatchedEpisodes =
            animeDetailComplementDao.getAllAnimeDetailsWithWatchedEpisodes()

        val unfinishedEpisodes = animeDetailsWithWatchedEpisodes.mapNotNull { animeDetail ->
            val episodeId = animeDetail.lastEpisodeWatchedId ?: return@mapNotNull null
            val episode = episodeDetailComplementDao.getEpisodeDetailComplementById(episodeId)
                ?: return@mapNotNull null

            if (episode.lastWatched != null &&
                episode.lastTimestamp != null &&
                episode.duration != null &&
                episode.lastTimestamp < episode.duration &&
                animeDetail.episodes != null &&
                episode.number < animeDetail.episodes.size
            ) {
                Pair(episode, animeDetail.episodes.size - episode.number)
            } else {
                null
            }
        }

        return if (unfinishedEpisodes.isNotEmpty()) {
            unfinishedEpisodes.random()
        } else {
            Pair(null, 0)
        }
    }
}