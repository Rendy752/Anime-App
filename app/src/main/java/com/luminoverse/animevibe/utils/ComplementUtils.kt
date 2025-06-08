package com.luminoverse.animevibe.utils

import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.AnimeDetailComplement
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeServersResponse
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.EpisodeSourcesResponse
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

        val animeDetailResult = repository.getAnimeDetail(malId)
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
        isRefresh: Boolean = false
    ): AnimeDetailComplement? = withContext(Dispatchers.IO) {
        if (!TimeUtils.isEpisodeAreUpToDate(
                animeDetail.broadcast.time,
                animeDetail.broadcast.timezone,
                animeDetail.broadcast.day,
                animeDetailComplement.lastEpisodeUpdatedAt
            ) || isRefresh
        ) {
            val episodesResponse = repository.getEpisodes(animeDetailComplement.id)
            if (episodesResponse is Resource.Success) {
                val episodes = episodesResponse.data.episodes
                if (episodes != animeDetailComplement.episodes) {
                    val updatedAnimeDetail = animeDetailComplement.copy(
                        episodes = episodes,
                        lastEpisodeUpdatedAt = Instant.now().epochSecond
                    )
                    repository.updateCachedAnimeDetailComplement(updatedAnimeDetail)
                    return@withContext updatedAnimeDetail
                }
            }
        }
        animeDetailComplement
    }

    /**
     * Creates and caches an EpisodeDetailComplement from episode data.
     */
    suspend fun createEpisodeDetailComplement(
        repository: AnimeEpisodeDetailRepository,
        animeDetailMalId: Int,
        animeDetailTitle: String,
        animeDetailImageUrl: String?,
        animeDetailComplement: AnimeDetailComplement,
        episode: Episode,
        servers: EpisodeServersResponse,
        sources: EpisodeSourcesResponse,
        sourcesQuery: EpisodeSourcesQuery
    ): EpisodeDetailComplement = withContext(Dispatchers.IO) {
        val complement = EpisodeDetailComplement(
            id = episode.episodeId,
            malId = animeDetailMalId,
            aniwatchId = animeDetailComplement.id,
            animeTitle = animeDetailTitle,
            episodeTitle = episode.name,
            imageUrl = animeDetailImageUrl,
            number = episode.episodeNo,
            isFiller = episode.filler,
            servers = servers,
            sources = sources,
            sourcesQuery = sourcesQuery
        )
        repository.insertCachedEpisodeDetailComplement(complement)
        complement
    }

    /**
     * Toggles favorite status for an AnimeDetailComplement.
     */
    suspend fun toggleAnimeFavorite(
        repository: AnimeEpisodeDetailRepository,
        id: String? = null,
        malId: Int,
        isFavorite: Boolean
    ): AnimeDetailComplement? = withContext(Dispatchers.IO) {
        val animeComplement = getOrCreateAnimeDetailComplement(
            repository = repository,
            id = id,
            malId = malId,
            isFavorite = isFavorite
        )
        if (animeComplement != null) {
            val updatedAnime = animeComplement.copy(isFavorite = isFavorite)
            repository.updateCachedAnimeDetailComplement(updatedAnime)
            updatedAnime
        } else {
            null
        }
    }

    /**
     * Toggles favorite status for an EpisodeDetailComplement.
     */
    suspend fun toggleEpisodeFavorite(
        repository: AnimeEpisodeDetailRepository,
        episodeId: String,
        isFavorite: Boolean
    ) = withContext(Dispatchers.IO) {
        val episode = repository.getCachedEpisodeDetailComplement(episodeId)
        if (episode != null) {
            val updatedEpisode = episode.copy(isFavorite = isFavorite)
            repository.updateEpisodeDetailComplement(updatedEpisode)
        }
    }
}