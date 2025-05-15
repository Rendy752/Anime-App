package com.luminoverse.animevibe.repository

import com.luminoverse.animevibe.data.remote.api.AnimeAPI
import com.luminoverse.animevibe.models.AnimeSchedulesSearchQueryState
import com.luminoverse.animevibe.models.ListAnimeDetailResponse
import com.luminoverse.animevibe.utils.Resource
import com.luminoverse.animevibe.utils.ResponseHandler
import com.luminoverse.animevibe.utils.ResponseHandler.safeApiCall
import com.luminoverse.animevibe.utils.TimeUtils

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