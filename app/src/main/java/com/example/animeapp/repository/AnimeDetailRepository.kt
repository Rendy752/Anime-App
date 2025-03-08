package com.example.animeapp.repository

import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.local.dao.AnimeDetailComplementDao
import com.example.animeapp.data.local.dao.EpisodeDetailComplementDao
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AnimeDetailRepository(
    private val animeDetailDao: AnimeDetailDao,
    private val animeDetailComplementDao: AnimeDetailComplementDao,
    private val episodeDetailComplementDao: EpisodeDetailComplementDao,
    private val animeAPI: AnimeAPI
) {
    suspend fun getAnimeDetail(id: Int): Response<AnimeDetailResponse> =
        withContext(Dispatchers.IO) {
            getCachedAnimeDetailResponse(id) ?: getRemoteAnimeDetail(id)
        }

    private suspend fun getCachedAnimeDetailResponse(id: Int): Response<AnimeDetailResponse>? {
        val cachedAnimeDetail = animeDetailDao.getAnimeDetailById(id)
        return cachedAnimeDetail?.let { cache ->
            val remoteData =
                ResponseHandler.handleCommonResponse(animeAPI.getAnimeDetail(cache.mal_id))
            if (remoteData is Resource.Success && remoteData.data?.data?.airing != cache.airing) {
                remoteData.data?.data?.let {
                    animeDetailDao.updateAnimeDetail(it)
                    return Response.success(remoteData.data)
                }
            }
            Response.success(AnimeDetailResponse(cache))
        } ?: getRemoteAnimeDetail(id)
    }

    private suspend fun getRemoteAnimeDetail(id: Int): Response<AnimeDetailResponse> {
        try {
            val response = animeAPI.getAnimeDetail(id)
            val body = response.body()
            return if (response.isSuccessful && body != null) {
                body.data.let {
                    animeDetailDao.insertAnimeDetail(it)
                }
                Response.success(body)
            } else if (response.isSuccessful) {
                Response.success(null)
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

    suspend fun getCachedAnimeDetailComplementByMalId(mal_id: Int): AnimeDetailComplement? =
        withContext(Dispatchers.IO) {
            animeDetailComplementDao.getAnimeDetailComplementByMalId(mal_id)
        }

    suspend fun insertCachedAnimeDetailComplement(animeDetailComplement: AnimeDetailComplement) =
        withContext(Dispatchers.IO) {
            animeDetailComplementDao.insertAnimeDetailComplement(animeDetailComplement)
        }

    suspend fun updateCachedAnimeDetailComplement(animeDetailComplement: AnimeDetailComplement) =
        withContext(Dispatchers.IO) {
            animeDetailComplementDao.updateAnimeDetailComplement(animeDetailComplement)
        }

    suspend fun getCachedEpisodeDetailComplement(id: String): EpisodeDetailComplement? =
        withContext(Dispatchers.IO) {
            episodeDetailComplementDao.getEpisodeDetailComplementById(id)
        }

    suspend fun insertCachedEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement) =
        withContext(Dispatchers.IO) {
            episodeDetailComplementDao.insertEpisodeDetailComplement(episodeDetailComplement)
        }
}