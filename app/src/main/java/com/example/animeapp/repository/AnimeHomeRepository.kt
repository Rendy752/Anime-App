package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeSchedulesSearchQueryState
import com.example.animeapp.models.ListAnimeDetailResponse
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import com.example.animeapp.utils.ResponseHandler.safeApiCall
import com.example.animeapp.utils.TimeUtils

class AnimeHomeRepository(
    private val jikanAPI: AnimeAPI
) {
    suspend fun getAnimeSchedules(
        queryState: AnimeSchedulesSearchQueryState
    ): Resource<ListAnimeDetailResponse> {
        queryState.apply {
            val response = safeApiCall {
                jikanAPI.getAnimeSchedules(
                    filter = filter,
                    sfw = sfw,
                    kids = kids,
                    unapproved = unapproved,
                    page = page,
                    limit = limit
                )
            }
            val handledResponse = ResponseHandler.handleCommonResponse(response)

            return when (handledResponse) {
                is Resource.Success -> Resource.Success(
                    handledResponse.data.copy(
                        data = handledResponse.data.data
                            .distinctBy { it.mal_id }
                            .sortedBy { anime ->
                                TimeUtils.getBroadcastDateTimeForSorting(
                                    broadcastTime = anime.broadcast.time,
                                    broadcastTimezone = anime.broadcast.timezone,
                                    broadcastDay = anime.broadcast.day
                                )?.toInstant()?.toEpochMilli() ?: Long.MAX_VALUE
                            }
                    )
                )

                else -> handledResponse
            }
        }
    }

    suspend fun getTop10Anime(): Resource<ListAnimeDetailResponse> {
        val response = safeApiCall { jikanAPI.getTop20Anime() }
        val handledResponse = ResponseHandler.handleCommonResponse(response)

        return when (handledResponse) {
            is Resource.Success -> Resource.Success(
                handledResponse.data.copy(
                    data = handledResponse.data.data.distinctBy { it.mal_id }.take(10)
                )
            )

            else -> handledResponse
        }
    }
}