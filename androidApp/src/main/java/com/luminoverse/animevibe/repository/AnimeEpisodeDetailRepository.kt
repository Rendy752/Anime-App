package com.luminoverse.animevibe.repository

import com.luminoverse.animevibe.data.local.dao.AnimeDetailDao
import com.luminoverse.animevibe.data.local.dao.AnimeDetailComplementDao
import com.luminoverse.animevibe.data.local.dao.EpisodeDetailComplementDao
import com.luminoverse.animevibe.data.remote.api.AnimeAPI
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.ui.common.AnimeAniwatchCommonResponse
import com.luminoverse.animevibe.utils.TimeUtils
import com.luminoverse.animevibe.utils.media.StreamingUtils
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.resource.ResponseHandler
import com.luminoverse.animevibe.utils.resource.ResponseHandler.safeApiCall
import com.luminoverse.animevibe.utils.watch.AnimeTitleFinder
import com.luminoverse.animevibe.utils.watch.AnimeTitleFinder.normalizeTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.math.ceil

sealed class LoadEpisodesResult {
    data class Success(val complement: AnimeDetailComplement, val newEpisodeIds: List<String> = emptyList()) : LoadEpisodesResult()
    data class Error(val message: String) : LoadEpisodesResult()
}

data class EpisodeHistoryResult(
    val data: Map<AnimeDetailComplement, List<EpisodeDetailComplement>>,
    val pagination: CompletePagination
)

class AnimeEpisodeDetailRepository(
    private val animeDetailDao: AnimeDetailDao,
    private val animeDetailComplementDao: AnimeDetailComplementDao,
    private val episodeDetailComplementDao: EpisodeDetailComplementDao,
    private val jikanAPI: AnimeAPI,
    private val runwayAPI: AnimeAPI
) {
    private val episodeExtractors = listOf<(EpisodeDetailComplement) -> String>(
        { it.episodeTitle },
        { it.animeTitle }
    )

    private suspend fun getOrCreateAnimeDetailComplement(
        id: String? = null,
        malId: Int,
        isFavorite: Boolean = false
    ): AnimeDetailComplement? = withContext(Dispatchers.IO) {
        getCachedAnimeDetailComplementByMalId(malId)?.let { return@withContext it }

        val (animeDetailResult, _) = getAnimeDetail(malId)
        if (animeDetailResult is Resource.Success) {
            val animeDetail = animeDetailResult.data.data
            val animeComplement = AnimeDetailComplement(
                id = id ?: animeDetail.mal_id.toString(),
                malId = animeDetail.mal_id,
                isFavorite = isFavorite,
                eps = animeDetail.episodes
            )
            insertCachedAnimeDetailComplement(animeComplement)
            return@withContext animeComplement
        }
        null
    }

    private suspend fun updateAnimeDetailComplementWithEpisodes(
        animeDetail: AnimeDetail,
        animeDetailComplement: AnimeDetailComplement,
        isRefresh: Boolean
    ): AnimeDetailComplement? = withContext(Dispatchers.IO) {
        val isDataNeedUpdate = isDataNeedUpdate(animeDetail, animeDetailComplement)
        if (!isDataNeedUpdate && !isRefresh || animeDetailComplement.id.all { it.isDigit() }) return@withContext animeDetailComplement

        val episodesResponse = getEpisodes(animeDetailComplement.id)
        if (episodesResponse is Resource.Success) {
            val episodes = episodesResponse.data.results.episodes
            if (episodes != animeDetailComplement.episodes) {
                val updatedAnimeDetail = animeDetailComplement.copy(
                    episodes = episodes,
                    lastEpisodeUpdatedAt = Instant.now().epochSecond
                )
                updateCachedAnimeDetailComplement(updatedAnimeDetail)
                return@withContext updatedAnimeDetail
            }
        }
        animeDetailComplement
    }

    suspend fun getPaginatedAndFilteredHistory(queryState: EpisodeHistoryQueryState): Resource<EpisodeHistoryResult> {
        return try {
            val allHistory = episodeDetailComplementDao.getAllEpisodeHistory(
                isFavorite = queryState.isFavorite,
                sortBy = queryState.sortBy.name
            )

            val searchedHistory = if (queryState.searchQuery.isNotBlank()) {
                AnimeTitleFinder.searchTitle(
                    searchQuery = queryState.searchQuery,
                    items = allHistory,
                    extractors = episodeExtractors
                )
            } else {
                allHistory
            }

            val groupedByAnime = searchedHistory.groupBy { it.malId }
                .mapNotNull { (malId, episodes) ->
                    getOrCreateAnimeDetailComplement(malId = malId)
                        ?.let { complement -> complement to episodes }
                }.toMap()

            if (groupedByAnime.isEmpty()) {
                return Resource.Success(EpisodeHistoryResult(emptyMap(), defaultCompletePagination))
            }

            val totalEpisodes = groupedByAnime.values.sumOf { it.size }
            val lastVisiblePage = ceil(totalEpisodes.toDouble() / queryState.limit).toInt().coerceAtLeast(1)
            val adjustedPage = queryState.page.coerceAtMost(lastVisiblePage)
            val offset = (adjustedPage - 1) * queryState.limit

            val paginatedEpisodes = groupedByAnime.entries.flatMap { it.value }.drop(offset).take(queryState.limit)
            val paginatedMap = paginatedEpisodes.groupBy { it.malId }
                .mapNotNull { (malId, episodes) ->
                    groupedByAnime.keys.find { it.malId == malId }?.let { it to episodes }
                }.toMap()

            val pagination = CompletePagination(
                last_visible_page = lastVisiblePage,
                has_next_page = adjustedPage < lastVisiblePage,
                current_page = adjustedPage,
                items = Items(
                    count = paginatedMap.values.sumOf { it.size },
                    total = totalEpisodes,
                    per_page = queryState.limit
                )
            )

            Resource.Success(EpisodeHistoryResult(paginatedMap, pagination))
        } catch (e: Exception) {
            Resource.Error("Failed to fetch episode history: ${e.message}")
        }
    }

    suspend fun loadAllEpisodes(
        animeDetail: AnimeDetail,
        isRefresh: Boolean
    ): LoadEpisodesResult {
        if (animeDetail.type == "Music") {
            val complement = getOrCreateAnimeDetailComplement(malId = animeDetail.mal_id)
            return if (complement != null) LoadEpisodesResult.Success(complement)
            else LoadEpisodesResult.Error("Failed to create anime complement for Music type")
        }

        val cachedComplement = getCachedAnimeDetailComplementByMalId(animeDetail.mal_id)
        if (cachedComplement != null) {
            val oldEpisodes = cachedComplement.episodes ?: emptyList()
            val updatedComplement = updateAnimeDetailComplementWithEpisodes(
                animeDetail = animeDetail,
                animeDetailComplement = cachedComplement,
                isRefresh = isRefresh
            )
            if (updatedComplement != null) {
                if (isRefresh && !updatedComplement.id.all { it.isDigit() }) {
                    val newEpisodeIds = (updatedComplement.episodes ?: emptyList())
                        .filter { newEp -> oldEpisodes.none { it.id == newEp.id } }
                        .map { it.id }
                    return LoadEpisodesResult.Success(updatedComplement, newEpisodeIds)
                }
                return LoadEpisodesResult.Success(updatedComplement)
            }
        }

        val searchTitles = listOfNotNull(animeDetail.title_english, animeDetail.title).distinct()
        var relatedAnime: AnimeAniwatch? = null

        for (title in searchTitles) {
            when (val searchResponse = getAnimeAniwatchSearch(title.normalizeTitle())) {
                is Resource.Success -> {
                    val foundAnime = searchResponse.data.results.data.find { it.malID == animeDetail.mal_id }
                    if (foundAnime != null) {
                        relatedAnime = foundAnime
                        break
                    }
                }
                is Resource.Error -> return LoadEpisodesResult.Error(searchResponse.message)
                else -> continue
            }
        }

        val finalRelatedAnime = relatedAnime ?: run {
            // Updated call
            val complement = getOrCreateAnimeDetailComplement(malId = animeDetail.mal_id)
            return if (complement != null) LoadEpisodesResult.Success(complement)
            else LoadEpisodesResult.Error("Anime not found and failed to create complement")
        }

        val episodesResource = getEpisodes(finalRelatedAnime.id)
        // Updated call
        val complement = getOrCreateAnimeDetailComplement(
            id = finalRelatedAnime.id,
            malId = animeDetail.mal_id
        )?.copy(
            id = finalRelatedAnime.id,
            episodes = if (episodesResource is Resource.Success) episodesResource.data.results.episodes else null,
            eps = finalRelatedAnime.tvInfo.eps,
            sub = finalRelatedAnime.tvInfo.sub,
            dub = finalRelatedAnime.tvInfo.dub
        )

        return if (complement != null) {
            updateCachedAnimeDetailComplement(complement)
            LoadEpisodesResult.Success(complement)
        } else {
            LoadEpisodesResult.Error("Failed to create and update complement from search")
        }
    }

    suspend fun toggleAnimeFavorite(
        id: String?,
        malId: Int,
        isFavorite: Boolean
    ): AnimeDetailComplement? {
        val animeComplement = getOrCreateAnimeDetailComplement(
            id = id,
            malId = malId,
            isFavorite = isFavorite
        )
        return if (animeComplement != null) {
            val updatedAnime = animeComplement.copy(isFavorite = isFavorite)
            updateCachedAnimeDetailComplement(updatedAnime)
            updatedAnime
        } else {
            null
        }
    }

    suspend fun toggleEpisodeFavorite(episodeId: String, isFavorite: Boolean): Resource<Unit> {
        return try {
            val episode = getCachedEpisodeDetailComplement(episodeId)
                ?: return Resource.Error("Episode not found")
            val updatedEpisode = episode.copy(isFavorite = isFavorite)
            updateEpisodeDetailComplement(updatedEpisode)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to update favorite status: ${e.message}")
        }
    }

    suspend fun getAnimeDetail(id: Int): Pair<Resource<AnimeDetailResponse>, Boolean> {
        val cached = getCachedAnimeDetailById(id)
        if (cached != null && !isDataNeedUpdate(cached)) {
            return Pair(Resource.Success(AnimeDetailResponse(cached)), true)
        }

        val resource = getUpdatedAnimeDetailById(id)
        return Pair(resource, false)
    }

    suspend fun getCachedAnimeDetailById(id: Int): AnimeDetail? =
        animeDetailDao.getAnimeDetailById(id)

    suspend fun getUpdatedAnimeDetailById(id: Int): Resource<AnimeDetailResponse> {
        val resource =
            ResponseHandler.handleCommonResponse(safeApiCall { jikanAPI.getAnimeDetail(id) })
        if (resource is Resource.Success) {
            animeDetailDao.insertAnimeDetail(resource.data.data)
        }
        return resource
    }

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

    suspend fun getAllFavoriteAnimeComplements(): List<AnimeDetailComplement> =
        animeDetailComplementDao.getAllFavoriteAnimeComplements()

    suspend fun insertCachedAnimeDetailComplement(animeDetailComplement: AnimeDetailComplement) =
        animeDetailComplementDao.insertAnimeDetailComplement(animeDetailComplement)

    suspend fun updateCachedAnimeDetailComplement(updatedAnimeDetailComplement: AnimeDetailComplement) {
        val complementToReplace =
            updatedAnimeDetailComplement.copy(updatedAt = Instant.now().epochSecond)
        animeDetailComplementDao.replaceByMalId(complementToReplace)
    }

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

    suspend fun getEpisodeStreamingDetails(
        episodeSourcesQuery: EpisodeSourcesQuery,
        isRefresh: Boolean,
        errorSourceQueryList: List<EpisodeSourcesQuery>,
        animeDetail: AnimeDetail?,
        animeDetailComplement: AnimeDetailComplement?
    ): Resource<EpisodeDetailComplement> {
        if (!isRefresh) {
            val cachedEpisode = getCachedEpisodeDetailComplement(episodeSourcesQuery.id)
            if (cachedEpisode != null && cachedEpisode.sourcesQuery == episodeSourcesQuery) {
                return Resource.Success(cachedEpisode)
            }
        }

        if (animeDetail == null || animeDetailComplement == null) {
            return Resource.Error("Anime details not loaded.")
        }

        val serversResource = getEpisodeServers(episodeSourcesQuery.id)
        if (serversResource !is Resource.Success) {
            return Resource.Error("Failed to fetch episode servers.")
        }

        val (sourcesResource, finalQuery) = StreamingUtils.getEpisodeSourcesResult(
            episodeId = episodeSourcesQuery.id,
            episodeServers = serversResource.data.results,
            getEpisodeSources = ::getEpisodeSources,
            errorSourceQueryList = errorSourceQueryList,
            episodeSourcesQuery = episodeSourcesQuery
        )

        if (sourcesResource !is Resource.Success || finalQuery == null) {
            return Resource.Error(sourcesResource.message ?: "Could not find any working servers.")
        }

        val existingComplement = getCachedEpisodeDetailComplement(episodeSourcesQuery.id)
        val finalComplement = if (existingComplement != null) {
            existingComplement.copy(
                servers = serversResource.data.results,
                sources = sourcesResource.data.results.streamingLink,
                sourcesQuery = finalQuery
            )
        } else {
            val episodeInfo = animeDetailComplement.episodes?.firstOrNull { it.id == episodeSourcesQuery.id }
                ?: return Resource.Error("Episode info not found in complement.")

            EpisodeDetailComplement(
                id = episodeInfo.id,
                malId = animeDetail.mal_id,
                aniwatchId = animeDetailComplement.id,
                animeTitle = animeDetail.title,
                episodeTitle = episodeInfo.title,
                imageUrl = animeDetail.images.webp.large_image_url,
                number = episodeInfo.episode_no,
                isFiller = episodeInfo.filler,
                servers = serversResource.data.results,
                sources = sourcesResource.data.results.streamingLink,
                sourcesQuery = finalQuery
            )
        }

        insertCachedEpisodeDetailComplement(finalComplement)
        return Resource.Success(finalComplement)
    }

    suspend fun getAnimeAniwatchSearch(keyword: String) =
        ResponseHandler.handleCommonResponse(safeApiCall { runwayAPI.getAnimeAniwatchSearch(keyword) })

    suspend fun getEpisodeServers(id: String): Resource<AnimeAniwatchCommonResponse<List<EpisodeServer>>> {
        val animeId = id.split("?ep=").first()
        val episodeId = id.split("?ep=").last()
        return ResponseHandler.handleCommonResponse(safeApiCall {
            runwayAPI.getEpisodeServers(
                animeId,
                episodeId
            )
        })
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