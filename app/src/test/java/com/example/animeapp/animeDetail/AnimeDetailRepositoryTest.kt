package com.example.animeapp.animeDetail

import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.local.dao.AnimeDetailComplementDao
import com.example.animeapp.data.local.dao.EpisodeDetailComplementDao
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodesResponse
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.utils.TimeUtils
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AnimeDetailRepositoryTest {

    private lateinit var animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
    private val animeDetailDao: AnimeDetailDao = mockk()
    private val animeDetailComplementDao: AnimeDetailComplementDao = mockk()
    private val episodeDetailComplementDao: EpisodeDetailComplementDao = mockk()
    private val jikanAPI: AnimeAPI = mockk()
    private val runwayAPI: AnimeAPI = mockk()

    @Before
    fun setup() {
        animeEpisodeDetailRepository = AnimeEpisodeDetailRepository(
            animeDetailDao,
            animeDetailComplementDao,
            episodeDetailComplementDao,
            jikanAPI,
            runwayAPI
        )
    }

    @Test
    fun `getAnimeDetail should return cached data when available and up to date`() = runBlocking {
        val animeId = 123
        val animeDetail = mockk<AnimeDetail>()
        val animeDetailResponse = AnimeDetailResponse(animeDetail)
        coEvery { animeDetailDao.getAnimeDetailById(animeId) } returns animeDetail
        every { TimeUtils.isEpisodeAreUpToDate(any(), any(), any(), any()) } returns true

        val result = animeEpisodeDetailRepository.getAnimeDetail(animeId)

        assertEquals(Response.success(animeDetailResponse), result)
    }

    @Test
    fun `getAnimeDetail should return remote data when cache is outdated`() = runBlocking {
        val animeId = 123
        val animeDetail = mockk<AnimeDetail>()
        val animeDetailResponse = AnimeDetailResponse(animeDetail)
        coEvery { animeDetailDao.getAnimeDetailById(animeId) } returns animeDetail
        every { TimeUtils.isEpisodeAreUpToDate(any(), any(), any(), any()) } returns false
        coEvery { jikanAPI.getAnimeDetail(animeId) } returns Response.success(animeDetailResponse)
        coEvery { animeDetailDao.updateAnimeDetail(any()) } returns Unit

        val result = animeEpisodeDetailRepository.getAnimeDetail(animeId)

        assertEquals(Response.success(animeDetailResponse), result)
    }

    @Test
    fun `getAnimeDetail should return remote data when cache is not available`() = runBlocking {
        val animeId = 123
        val animeDetailResponse = AnimeDetailResponse(mockk())
        coEvery { animeDetailDao.getAnimeDetailById(animeId) } returns null
        coEvery { jikanAPI.getAnimeDetail(animeId) } returns Response.success(animeDetailResponse)
        coEvery { animeDetailDao.insertAnimeDetail(any()) } returns Unit

        val result = animeEpisodeDetailRepository.getAnimeDetail(animeId)

        assertEquals(Response.success(animeDetailResponse), result)
    }

    @Test
    fun `getCachedAnimeDetailComplementByMalId should return cached data`() = runBlocking {
        val malId = 123
        val animeDetailComplement = mockk<AnimeDetailComplement>()
        coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(malId) } returns animeDetailComplement

        val result = animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(malId)

        assertEquals(animeDetailComplement, result)
    }

    @Test
    fun `insertCachedAnimeDetailComplement should insert data`() = runBlocking {
        val animeDetailComplement = mockk<AnimeDetailComplement>()
        coEvery { animeDetailComplementDao.insertAnimeDetailComplement(animeDetailComplement) } returns Unit

        animeEpisodeDetailRepository.insertCachedAnimeDetailComplement(animeDetailComplement)
    }

    @Test
    fun `updateAnimeDetailComplementWithEpisodes should return updated data when needed and successful`() =
        runBlocking {
            val animeDetail = mockk<AnimeDetail>()
            val animeDetailComplement = mockk<AnimeDetailComplement>()
            coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(any()) } returns animeDetailComplement
            every { TimeUtils.isEpisodeAreUpToDate(any(), any(), any(), any()) } returns false
            coEvery { runwayAPI.getEpisodes(any()) } returns Response.success(mockk())
            coEvery { animeDetailComplementDao.updateEpisodeAnimeDetailComplement(any()) } returns Unit
            every {
                animeDetailComplement.copy(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns animeDetailComplement

            val result = animeEpisodeDetailRepository.updateAnimeDetailComplementWithEpisodes(
                animeDetail,
                animeDetailComplement
            )

            assertEquals(animeDetailComplement, result)
        }

    @Test
    fun `updateAnimeDetailComplementWithEpisodes should return cached data when not needed`() =
        runBlocking {
            val animeDetail = mockk<AnimeDetail>()
            val animeDetailComplement = mockk<AnimeDetailComplement>()
            coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(any()) } returns animeDetailComplement
            every { TimeUtils.isEpisodeAreUpToDate(any(), any(), any(), any()) } returns true

            val result = animeEpisodeDetailRepository.updateAnimeDetailComplementWithEpisodes(
                animeDetail,
                animeDetailComplement
            )

            assertEquals(animeDetailComplement, result)
        }

    @Test
    fun `updateAnimeDetailComplementWithEpisodes should return cached object when api call fails`() =
        runBlocking {
            val animeDetail = mockk<AnimeDetail> {
                every { airing } returns false
            }
            val animeDetailComplement = mockk<AnimeDetailComplement>()
            val responseBody = mockk<okhttp3.ResponseBody> {
                every { contentType() } returns "application/json".toMediaTypeOrNull()
                every { contentLength() } returns 0L
            }
            val errorResponse = Response.error<EpisodesResponse>(400, responseBody)
            coEvery { runwayAPI.getEpisodes(any()) } returns errorResponse

            val result = animeEpisodeDetailRepository.updateAnimeDetailComplementWithEpisodes(
                animeDetail,
                animeDetailComplement
            )

            assertEquals(animeDetailComplement, result)
        }

    @Test
    fun `getCachedEpisodeDetailComplement should return cached data`() = runBlocking {
        val episodeId = "123"
        val episodeDetailComplement = mockk<EpisodeDetailComplement>()
        coEvery { episodeDetailComplementDao.getEpisodeDetailComplementById(episodeId) } returns episodeDetailComplement

        val result = animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId)

        assertEquals(episodeDetailComplement, result)
    }

    @Test
    fun `insertCachedEpisodeDetailComplement should insert data`() = runBlocking {
        val episodeDetailComplement = mockk<EpisodeDetailComplement>()
        coEvery { episodeDetailComplementDao.insertEpisodeDetailComplement(episodeDetailComplement) } returns Unit

        animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(episodeDetailComplement)
    }
}