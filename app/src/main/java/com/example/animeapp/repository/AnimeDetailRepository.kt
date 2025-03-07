package com.example.animeapp.repository

import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.local.dao.AnimeDetailComplementDao
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.AnimeDetailResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AnimeDetailRepository(
    private val animeDetailDao: AnimeDetailDao,
    private val animeDetailComplementDao: AnimeDetailComplementDao,
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
        try {
            val response = animeAPI.getAnimeDetail(id)
            if (response.isSuccessful) {
                val animeDetailData = response.body()?.data
                if (animeDetailData != null) {
                    animeDetailDao.insertAnimeDetail(animeDetailData)
                    return response
                } else {
                    return Response.success(null)
                }
            } else {
                return response
            }
        } catch (e: IOException) {
            return Response.error(500, "Network error".toResponseBody())
        } catch (e: HttpException) {
            return Response.error(e.code(), "HTTP error".toResponseBody())
        } catch (e: Exception) {
            return Response.error(500, "Unknown error".toResponseBody())
        }
    }

    suspend fun getCachedAnimeDetailComplement(id: String): AnimeDetailComplement? = withContext(Dispatchers.IO) {
        animeDetailComplementDao.getAnimeDetailComplementById(id)
    }

    suspend fun getCachedAnimeDetailComplementByMalId(mal_id: Int): AnimeDetailComplement? = withContext(Dispatchers.IO) {
        animeDetailComplementDao.getAnimeDetailComplementByMalId(mal_id)
    }

    suspend fun insertCachedAnimeDetailComplement(animeDetailComplement: AnimeDetailComplement) = withContext(Dispatchers.IO) {
        animeDetailComplementDao.insertAnimeDetailComplement(animeDetailComplement)
    }
}