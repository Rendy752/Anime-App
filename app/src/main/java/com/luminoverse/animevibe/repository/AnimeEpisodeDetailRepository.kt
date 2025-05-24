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
import com.luminoverse.animevibe.models.EpisodeServersResponse
import com.luminoverse.animevibe.models.EpisodesResponse
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.resource.ResponseHandler
import com.luminoverse.animevibe.utils.resource.ResponseHandler.safeApiCall
import com.luminoverse.animevibe.utils.TimeUtils
import java.time.Instant
import kotlin.random.Random

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

    private suspend fun isDataNeedUpdate(data: AnimeDetail): Boolean {
        val complement = getCachedAnimeDetailComplementByMalId(data.mal_id)
        return data.airing && TimeUtils.isEpisodeAreUpToDate(
            data.broadcast.time,
            data.broadcast.timezone,
            data.broadcast.day,
            complement?.lastEpisodeUpdatedAt
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

    suspend fun getCachedDefaultEpisodeDetailComplementByMalId(malId: Int): EpisodeDetailComplement =
        episodeDetailComplementDao.getDefaultEpisodeDetailComplementByMalId(malId)

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

    suspend fun getEpisodeServers(episodeId: String): Resource<EpisodeServersResponse> =
        ResponseHandler.handleCommonResponse(safeApiCall { runwayAPI.getEpisodeServers(episodeId) })

    suspend fun getEpisodeSources(episodeId: String, server: String, category: String) =
        ResponseHandler.handleCommonResponse(safeApiCall {
            runwayAPI.getEpisodeSources(
                episodeId, server, category
            )
        })

    suspend fun getEpisodes(id: String): Resource<EpisodesResponse> =
        ResponseHandler.handleCommonResponse(safeApiCall { runwayAPI.getEpisodes(id) })

    suspend fun getPaginatedEpisodeHistory(queryState: EpisodeHistoryQueryState): Resource<List<EpisodeDetailComplement>> {
        return try {
            Resource.Success(
                episodeDetailComplementDao.getPaginatedEpisodeHistory(
                    isFavorite = queryState.isFavorite,
                    sortBy = queryState.sortBy.name,
                    limit = queryState.limit,
                    offset = (queryState.page - 1) * queryState.limit
                )
            )
        } catch (e: Exception) {
            Resource.Error("Failed to fetch episode history: ${e.message}")
        }
    }

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

    suspend fun getEpisodeHistoryCount(isFavorite: Boolean?): Resource<Int> {
        return try {
            Resource.Success(
                episodeDetailComplementDao.getEpisodeHistoryCount(isFavorite)
            )
        } catch (e: Exception) {
            Resource.Error("Failed to fetch episode history count: ${e.message}")
        }
    }

    suspend fun getRandomCachedUnfinishedEpisode(): EpisodeDetailComplement? {
        val unfinishedEpisodes = episodeDetailComplementDao.getAllEpisodeHistory(
            isFavorite = null,
            sortBy = EpisodeHistoryQueryState.SortBy.UpdatedAt.name
        ).filter { episode ->
            val animeDetail = getCachedAnimeDetailComplementByMalId(episode.malId)
            episode.lastWatched != null &&
                    episode.lastTimestamp != null &&
                    episode.duration != null &&
                    episode.lastTimestamp < episode.duration &&
                    animeDetail?.episodes != null &&
                    episode.number < animeDetail.episodes.size
        }
        return if (unfinishedEpisodes.isNotEmpty()) {
            unfinishedEpisodes.random(Random(System.currentTimeMillis()))
        } else {
            null
        }
    }
}