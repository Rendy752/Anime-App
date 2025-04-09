package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeSeasonNowResponse
import com.example.animeapp.models.AnimeSeasonNowSearchQueryState
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import com.example.animeapp.utils.ResponseHandler.safeApiCall

class AnimeHomeRepository(
    private val jikanAPI: AnimeAPI
) {
    suspend fun getAnimeSeasonNow(
        queryState: AnimeSeasonNowSearchQueryState
    ): Resource<AnimeSeasonNowResponse> {
        queryState.apply {
            val response = safeApiCall {
                jikanAPI.getAnimeSeasonNow(
                    filter = filter,
                    sfw = sfw,
                    unapproved = unapproved,
                    continuing = continuing,
                    page = page,
                    limit = limit
                )
            }
            val handledResponse = ResponseHandler.handleCommonResponse(response)

            return when (handledResponse) {
                is Resource.Success -> Resource.Success(handledResponse.data.copy(data = handledResponse.data.data.distinctBy { it.mal_id }))
                else -> handledResponse
            }
        }
    }
}