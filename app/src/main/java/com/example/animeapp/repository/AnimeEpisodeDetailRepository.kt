package com.example.animeapp.repository

import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.local.dao.AnimeDetailComplementDao
import com.example.animeapp.data.local.dao.EpisodeDetailComplementDao
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodesResponse
import com.example.animeapp.utils.TimeUtils
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.animeapp.utils.ResponseHandler.safeApiCall
import java.time.Instant

class AnimeEpisodeDetailRepository(
    private val animeDetailDao: AnimeDetailDao,
    private val animeDetailComplementDao: AnimeDetailComplementDao,
    private val episodeDetailComplementDao: EpisodeDetailComplementDao,
    private val jikanAPI: AnimeAPI,
    private val runwayAPI: AnimeAPI
) {
    suspend fun getAnimeDetail(id: Int): Resource<AnimeDetailResponse> =
        withContext(Dispatchers.IO) {
            getCachedAnimeDetailResponse(id) ?: getRemoteAnimeDetail(id)
        }

    suspend fun getCachedAnimeDetailById(id: Int): AnimeDetail? =
        withContext(Dispatchers.IO) {
            animeDetailDao.getAnimeDetailById(id)
        }

    private suspend fun getCachedAnimeDetailResponse(id: Int): Resource<AnimeDetailResponse>? {
        val cachedAnimeDetail = getCachedAnimeDetailById(id)

        return cachedAnimeDetail?.let { cache ->
            if (isDataNeedUpdate(cache)) {
                val remoteData =
                    ResponseHandler.handleCommonResponse(jikanAPI.getAnimeDetail(cache.mal_id))

                if (remoteData is Resource.Success && remoteData.data.data != cache) {
                    remoteData.data.data.let {
                        animeDetailDao.updateAnimeDetail(it)
                        Resource.Success(remoteData.data)
                    }
                } else {
                    Resource.Success(AnimeDetailResponse(cache))
                }
            } else {
                Resource.Success(AnimeDetailResponse(cache))
            }
        }
    }

    private suspend fun isDataNeedUpdate(data: AnimeDetail, isRefresh: Boolean = false): Boolean {
        return data.airing && (isRefresh || !TimeUtils.isEpisodeAreUpToDate(
            data.broadcast.time,
            data.broadcast.timezone,
            data.broadcast.day,
            getCachedAnimeDetailComplementByMalId(data.mal_id)?.lastEpisodeUpdatedAt
        ))
    }

    private suspend fun getRemoteAnimeDetail(id: Int): Resource<AnimeDetailResponse> {
        val response = safeApiCall { jikanAPI.getAnimeDetail(id) }
        val resource = ResponseHandler.handleCommonResponse(response)
        if (resource is Resource.Success) animeDetailDao.insertAnimeDetail(resource.data.data)
        return resource
    }

    suspend fun getCachedAnimeDetailComplementByMalId(malId: Int): AnimeDetailComplement? =
        withContext(Dispatchers.IO) {
            animeDetailComplementDao.getAnimeDetailComplementByMalId(malId)
        }

    suspend fun insertCachedAnimeDetailComplement(animeDetailComplement: AnimeDetailComplement) =
        withContext(Dispatchers.IO) {
            animeDetailComplementDao.insertAnimeDetailComplement(animeDetailComplement)
        }

    suspend fun updateCachedAnimeDetailComplement(updatedAnimeDetailComplement: AnimeDetailComplement) =
        withContext(Dispatchers.IO) {
            animeDetailComplementDao.updateAnimeDetailComplement(
                updatedAnimeDetailComplement.copy(
                    updatedAt = Instant.now().epochSecond
                )
            )
        }

    suspend fun updateCachedAnimeDetailComplementWithEpisodes(
        animeDetail: AnimeDetail,
        cachedAnimeDetailComplement: AnimeDetailComplement,
        isRefresh: Boolean = false
    ): AnimeDetailComplement? = withContext(Dispatchers.IO) {
        if (isDataNeedUpdate(animeDetail, isRefresh)) {
            val episodesResponse = ResponseHandler.handleCommonResponse(
                runwayAPI.getEpisodes(cachedAnimeDetailComplement.id)
            )
            if (episodesResponse is Resource.Success) {
                val episodes = episodesResponse.data.episodes

                if (episodes != cachedAnimeDetailComplement.episodes) {
                    val updatedAnimeDetail = cachedAnimeDetailComplement.copy(episodes = episodes)
                    animeDetailComplementDao.updateAnimeDetailComplement(
                        updatedAnimeDetail.copy(
                            lastEpisodeUpdatedAt = Instant.now().epochSecond,
                            updatedAt = Instant.now().epochSecond
                        )
                    )
                    return@withContext updatedAnimeDetail
                } else {
                    return@withContext cachedAnimeDetailComplement
                }
            } else {
                return@withContext cachedAnimeDetailComplement
            }
        } else {
            return@withContext cachedAnimeDetailComplement
        }
    }

    suspend fun getCachedLatestWatchedEpisodeDetailComplement(): EpisodeDetailComplement? {
        return withContext(Dispatchers.IO) {
            episodeDetailComplementDao.getLatestWatchedEpisodeDetailComplement()
        }
    }

    suspend fun getCachedDefaultEpisodeDetailComplementByMalId(malId: Int): EpisodeDetailComplement =
        withContext(Dispatchers.IO) {
            episodeDetailComplementDao.getDefaultEpisodeDetailComplementByMalId(malId)
        }

    suspend fun getCachedEpisodeDetailComplement(id: String): EpisodeDetailComplement? =
        withContext(Dispatchers.IO) {
            episodeDetailComplementDao.getEpisodeDetailComplementById(id)
        }

    suspend fun insertCachedEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement) =
        withContext(Dispatchers.IO) {
            episodeDetailComplementDao.insertEpisodeDetailComplement(episodeDetailComplement)
        }

    suspend fun getAnimeAniwatchSearch(keyword: String) =
        safeApiCall { runwayAPI.getAnimeAniwatchSearch(keyword) }

    suspend fun getEpisodes(id: String): Resource<EpisodesResponse> {
        val response = safeApiCall { runwayAPI.getEpisodes(id) }
        return ResponseHandler.handleCommonResponse(response)
    }

    suspend fun getEpisodeServers(episodeId: String): Resource<EpisodeServersResponse> {
        val response = safeApiCall { runwayAPI.getEpisodeServers(episodeId) }
        return ResponseHandler.handleCommonResponse(response)
    }

    suspend fun getEpisodeSources(episodeId: String, server: String, category: String) =
        safeApiCall { runwayAPI.getEpisodeSources(episodeId, server, category) }

    suspend fun updateEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement) =
        withContext(Dispatchers.IO) {
            episodeDetailComplementDao.updateEpisodeDetailComplement(
                episodeDetailComplement.copy(
                    updatedAt = Instant.now().epochSecond
                )
            )
        }
}