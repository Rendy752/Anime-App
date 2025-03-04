package com.example.animeapp.repository

import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeDetailResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class AnimeDetailRepository(
    private val animeDetailDao: AnimeDetailDao,
    private val animeAPI: AnimeAPI
) {
    suspend fun getAnimeDetail(id: Int): Response<AnimeDetailResponse> =
        withContext(Dispatchers.IO) {
            getCachedAnimeDetailResponse(id) ?: getRemoteAnimeDetail(id)
        }

    private fun getCachedAnimeDetailResponse(id: Int): Response<AnimeDetailResponse>? {
        val cachedAnimeDetail = animeDetailDao.getAnimeDetailById(id)
        return cachedAnimeDetail?.let {
            Response.success(AnimeDetailResponse(it))
        }
    }

    private suspend fun getRemoteAnimeDetail(id: Int): Response<AnimeDetailResponse> {
        val response = animeAPI.getAnimeDetail(id)
        if (response.isSuccessful) {
            response.body()?.let { animeDetailResponse ->
                animeDetailDao.insertAnimeDetail(animeDetailResponse.data)
            }
        }
        return response
    }

    suspend fun getCachedAnimeDetail(id: Int): AnimeDetailResponse? {
        return withContext(Dispatchers.IO) {
            val cachedAnimeDetail = animeDetailDao.getAnimeDetailById(id)
            cachedAnimeDetail?.let { AnimeDetailResponse(it) }
        }
    }
}