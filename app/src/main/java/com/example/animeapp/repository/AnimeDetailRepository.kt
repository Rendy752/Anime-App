package com.example.animeapp.repository

import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.remote.api.RetrofitInstance
import com.example.animeapp.models.AnimeDetailResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class AnimeDetailRepository(private val animeDetailDao: AnimeDetailDao) {
    suspend fun getAnimeDetail(id: Int): Response<AnimeDetailResponse> = withContext(Dispatchers.IO) {
        val cachedAnimeDetail = animeDetailDao.getAnimeDetailById(id)
        if (cachedAnimeDetail != null) {
            val animeDetailResponse = AnimeDetailResponse(cachedAnimeDetail)
            Response.success(animeDetailResponse)
        } else {
            val response = RetrofitInstance.api.getAnimeDetail(id)
            if (response.isSuccessful) {
                response.body()?.let { animeDetailResponse ->
                    animeDetailDao.insertAnimeDetail(animeDetailResponse.data)
                }
            }
            response
        }
    }

    suspend fun getCachedAnimeDetail(id: Int): AnimeDetailResponse? {
        return withContext(Dispatchers.IO) {
            val cachedAnimeDetail = animeDetailDao.getAnimeDetailById(id)
            cachedAnimeDetail?.let { AnimeDetailResponse(it) }
        }
    }

    suspend fun cacheAnimeDetail(animeDetailResponse: AnimeDetailResponse) {
        animeDetailDao.insertAnimeDetail(animeDetailResponse.data)
    }
}