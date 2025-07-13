package com.luminoverse.animevibe.utils

import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.AnimeDetailComplement
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.utils.resource.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

object ComplementUtils {
    /**
     * Fetches or creates an AnimeDetailComplement for a given malId, using AnimeDetail as fallback.
     */
    suspend fun getOrCreateAnimeDetailComplement(
        repository: AnimeEpisodeDetailRepository,
        id: String? = null,
        malId: Int,
        isFavorite: Boolean = false
    ): AnimeDetailComplement? = withContext(Dispatchers.IO) {
        repository.getCachedAnimeDetailComplementByMalId(malId)?.let { return@withContext it }

        val (animeDetailResult, _) = repository.getAnimeDetail(malId)
        if (animeDetailResult is Resource.Success) {
            val animeDetail = animeDetailResult.data.data
            val animeComplement = AnimeDetailComplement(
                id = id ?: animeDetail.mal_id.toString(),
                malId = animeDetail.mal_id,
                isFavorite = isFavorite,
                eps = animeDetail.episodes
            )
            repository.insertCachedAnimeDetailComplement(animeComplement)
            return@withContext animeComplement
        }
        null
    }

    /**
     * Updates or creates an AnimeDetailComplement with episode data, considering broadcast schedule.
     */
    suspend fun updateAnimeDetailComplementWithEpisodes(
        repository: AnimeEpisodeDetailRepository,
        animeDetail: AnimeDetail,
        animeDetailComplement: AnimeDetailComplement,
        isRefresh: Boolean
    ): AnimeDetailComplement? = withContext(Dispatchers.IO) {
        val isDataNeedUpdate = repository.isDataNeedUpdate(animeDetail, animeDetailComplement)
        if (!isDataNeedUpdate && !isRefresh || animeDetailComplement.id.all { it.isDigit() }) return@withContext animeDetailComplement

        val episodesResponse = repository.getEpisodes(animeDetailComplement.id)
        if (episodesResponse is Resource.Success) {
            val episodes = episodesResponse.data.results.episodes
            if (episodes != animeDetailComplement.episodes) {
                val updatedAnimeDetail = animeDetailComplement.copy(
                    episodes = episodes,
                    lastEpisodeUpdatedAt = Instant.now().epochSecond
                )
                repository.updateCachedAnimeDetailComplement(updatedAnimeDetail)
                return@withContext updatedAnimeDetail
            }
        }
        animeDetailComplement
    }
}