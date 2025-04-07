package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.WatchRecentEpisodeResponse
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import com.example.animeapp.utils.ResponseHandler.safeApiCall

class AnimeHomeRepository(
    private val jikanAPI: AnimeAPI
) {
    suspend fun getWatchRecentEpisode(): Resource<WatchRecentEpisodeResponse> {
        val response = safeApiCall { jikanAPI.getWatchRecentEpisode() }
        return ResponseHandler.handleCommonResponse(response)
    }
}