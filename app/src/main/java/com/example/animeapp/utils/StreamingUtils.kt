package com.example.animeapp.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import com.example.animeapp.R
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.repository.AnimeStreamingRepository

object StreamingUtils {
    fun getEpisodeQuery(
        response: Resource<EpisodeServersResponse>,
        episodeId: String
    ): EpisodeSourcesQuery? {
        val episodeServers = response.data ?: return null
        return episodeServers.let {
            when {
                it.sub.isNotEmpty() -> EpisodeSourcesQuery(
                    episodeId,
                    it.sub.first().serverName,
                    "sub"
                )

                it.dub.isNotEmpty() -> EpisodeSourcesQuery(
                    episodeId,
                    it.dub.first().serverName,
                    "dub"
                )

                it.raw.isNotEmpty() -> EpisodeSourcesQuery(
                    episodeId,
                    it.raw.first().serverName,
                    "raw"
                )

                else -> null
            }
        }
    }

    suspend fun getEpisodeSources(
        response: Resource<EpisodeServersResponse>,
        animeStreamingRepository: AnimeStreamingRepository,
        episodeSourcesQuery: EpisodeSourcesQuery? = null
    ): Resource<EpisodeSourcesResponse> {
        val episodeServers = response.data
        val episodeDefaultId = episodeServers?.episodeId
            ?: return Resource.Error("No default episode found")

        val episodeQuery = episodeSourcesQuery ?: getEpisodeQuery(response, episodeDefaultId)
        ?: return Resource.Error("No episode servers found")

        return try {
            ResponseHandler.handleCommonResponse(
                animeStreamingRepository.getEpisodeSources(
                    episodeQuery.id,
                    episodeQuery.server,
                    episodeQuery.category
                )
            )
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }

    fun getEpisodeBackground(
        context: Context,
        episode: Episode,
        selectedEpisodeNo: Int? = null
    ): Drawable {
        val backgroundColor = if (episode.episodeNo == selectedEpisodeNo) {
            ContextCompat.getColor(context, R.color.selected_episode)
        } else if (episode.filler) {
            ContextCompat.getColor(context, R.color.filler_episode)
        } else {
            ContextCompat.getColor(context, R.color.default_episode)
        }

        val backgroundDrawable = GradientDrawable().apply {
            setColor(backgroundColor)
            cornerRadius = 16f
        }

        return LayerDrawable(arrayOf(backgroundDrawable))
    }
}