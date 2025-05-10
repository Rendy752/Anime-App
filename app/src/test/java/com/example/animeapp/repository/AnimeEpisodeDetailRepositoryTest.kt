package com.example.animeapp.repository

import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.local.dao.AnimeDetailComplementDao
import com.example.animeapp.data.local.dao.EpisodeDetailComplementDao
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.*
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import com.example.animeapp.utils.TimeUtils
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeEpisodeDetailRepositoryTest {

    private lateinit var repository: AnimeEpisodeDetailRepository
    private val animeDetailDao: AnimeDetailDao = mockk()
    private val animeDetailComplementDao: AnimeDetailComplementDao = mockk()
    private val episodeDetailComplementDao: EpisodeDetailComplementDao = mockk()
    private val jikanAPI: AnimeAPI = mockk()
    private val runwayAPI: AnimeAPI = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(TimeUtils)
        mockkObject(ResponseHandler)
        repository = AnimeEpisodeDetailRepository(
            animeDetailDao,
            animeDetailComplementDao,
            episodeDetailComplementDao,
            jikanAPI,
            runwayAPI
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(TimeUtils)
        unmockkObject(ResponseHandler)
        clearAllMocks()
    }

    @Test
    fun `getAnimeDetail returns cached data when available and up to date`() = runTest {
        val animeId = 1735
        val lastEpisodeUpdatedAt = Instant.now().epochSecond
        val broadcast = mockk<Broadcast>(relaxed = true) {
            every { time } returns "12:00"
            every { timezone } returns "Asia/Tokyo"
            every { day } returns "Monday"
        }
        val animeDetail = mockk<AnimeDetail>(relaxed = true) {
            every { mal_id } returns animeId
            every { airing } returns true
            every { this@mockk.broadcast } returns broadcast
        }
        val animeDetailResponse = AnimeDetailResponse(animeDetail)
        val animeDetailComplement = mockk<AnimeDetailComplement>(relaxed = true) {
            every { this@mockk.lastEpisodeUpdatedAt } returns lastEpisodeUpdatedAt
        }
        coEvery { animeDetailDao.getAnimeDetailById(animeId) } returns animeDetail
        coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(animeId) } returns animeDetailComplement
        coEvery {
            TimeUtils.isEpisodeAreUpToDate(
                "12:00",
                "Asia/Tokyo",
                "Monday",
                lastEpisodeUpdatedAt
            )
        } returns true

        val result = repository.getAnimeDetail(animeId)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Success) {
            println("Unexpected result: $result")
            if (result is Resource.Error) {
                println("Error message: ${result.message}")
            }
        }
        Assert.assertTrue(result is Resource.Success)
        Assert.assertEquals(animeDetailResponse, (result as Resource.Success).data)
        coVerify { animeDetailDao.getAnimeDetailById(animeId) }
        coVerify { animeDetailComplementDao.getAnimeDetailComplementByMalId(animeId) }
    }

    @Test
    fun `getAnimeDetail returns remote data when cache is not available`() = runTest {
        val animeId = 123
        val animeDetail = mockk<AnimeDetail>(relaxed = true) {
            every { mal_id } returns animeId
        }
        val animeDetailResponse = AnimeDetailResponse(animeDetail)
        coEvery { animeDetailDao.getAnimeDetailById(animeId) } returns null
        coEvery { jikanAPI.getAnimeDetail(animeId) } returns Response.success(animeDetailResponse)
        coEvery { ResponseHandler.handleCommonResponse(any<Response<AnimeDetailResponse>>()) } returns Resource.Success(
            animeDetailResponse
        )
        coEvery { animeDetailDao.insertAnimeDetail(animeDetail) } returns Unit

        val result = repository.getAnimeDetail(animeId)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Success) {
            println("Unexpected result: $result")
            if (result is Resource.Error) {
                println("Error message: ${result.message}")
            }
        }
        Assert.assertTrue(result is Resource.Success)
        Assert.assertEquals(animeDetailResponse, (result as Resource.Success).data)
        coVerify { animeDetailDao.getAnimeDetailById(animeId) }
        coVerify { jikanAPI.getAnimeDetail(animeId) }
        coVerify { animeDetailDao.insertAnimeDetail(animeDetail) }
    }

    @Test
    fun `getAnimeDetail returns remote data when cache is outdated`() = runTest {
        val animeId = 1735
        val lastEpisodeUpdatedAt = Instant.now().epochSecond - 3600
        val broadcast = mockk<Broadcast>(relaxed = true) {
            every { time } returns "12:00"
            every { timezone } returns "Asia/Tokyo"
            every { day } returns "Monday"
        }
        val animeDetail = mockk<AnimeDetail>(relaxed = true) {
            every { mal_id } returns animeId
            every { airing } returns true
            every { this@mockk.broadcast } returns broadcast
        }
        val newAnimeDetail = mockk<AnimeDetail>(relaxed = true) {
            every { mal_id } returns animeId
        }
        val animeDetailResponse = AnimeDetailResponse(newAnimeDetail)
        val animeDetailComplement = mockk<AnimeDetailComplement>(relaxed = true) {
            every { this@mockk.lastEpisodeUpdatedAt } returns lastEpisodeUpdatedAt
        }
        coEvery { animeDetailDao.getAnimeDetailById(animeId) } returns animeDetail
        coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(animeId) } returns animeDetailComplement
        coEvery {
            TimeUtils.isEpisodeAreUpToDate(
                "12:00",
                "Asia/Tokyo",
                "Monday",
                lastEpisodeUpdatedAt
            )
        } returns false
        coEvery { jikanAPI.getAnimeDetail(animeId) } returns Response.success(animeDetailResponse)
        coEvery { ResponseHandler.handleCommonResponse(any<Response<AnimeDetailResponse>>()) } returns Resource.Success(
            animeDetailResponse
        )
        coEvery { animeDetailDao.updateAnimeDetail(newAnimeDetail) } returns Unit

        val result = repository.getAnimeDetail(animeId)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Success) {
            println("Unexpected result: $result")
            if (result is Resource.Error) {
                println("Error message: ${result.message}")
            }
        }
        Assert.assertTrue(result is Resource.Success)
        Assert.assertEquals(animeDetailResponse, (result as Resource.Success).data)
        coVerify { animeDetailDao.getAnimeDetailById(animeId) }
        coVerify { animeDetailComplementDao.getAnimeDetailComplementByMalId(animeId) }
        coVerify { jikanAPI.getAnimeDetail(animeId) }
        coVerify { animeDetailDao.updateAnimeDetail(newAnimeDetail) }
    }

    @Test
    fun `getAnimeDetail returns error when remote call fails`() = runTest {
        val animeId = 123
        coEvery { animeDetailDao.getAnimeDetailById(animeId) } returns null
        coEvery { jikanAPI.getAnimeDetail(animeId) } returns Response.error(
            500,
            "Server error".toResponseBody()
        )
        coEvery { ResponseHandler.handleCommonResponse(any<Response<AnimeDetailResponse>>()) } returns Resource.Error(
            "Server error"
        )

        val result = repository.getAnimeDetail(animeId)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Error) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Error)
        Assert.assertEquals("Server error", (result as Resource.Error).message)
        coVerify { animeDetailDao.getAnimeDetailById(animeId) }
        coVerify { jikanAPI.getAnimeDetail(animeId) }
    }

    @Test
    fun `updateCachedAnimeDetailComplementWithEpisodes returns updated data when needed`() =
        runTest {
            val animeId = 123
            val lastEpisodeUpdatedAt = Instant.now().epochSecond - 3600
            val broadcast = mockk<Broadcast>(relaxed = true) {
                every { time } returns "12:00"
                every { timezone } returns "UTC"
                every { day } returns "Monday"
            }
            val animeDetail = mockk<AnimeDetail>(relaxed = true) {
                every { mal_id } returns animeId
                every { airing } returns true
                every { this@mockk.broadcast } returns broadcast
            }
            val cachedAnimeDetailComplement = mockk<AnimeDetailComplement>(relaxed = true) {
                every { id } returns "anime_123"
                every { episodes } returns listOf()
                every { this@mockk.lastEpisodeUpdatedAt } returns lastEpisodeUpdatedAt
            }
            coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(animeId) } returns cachedAnimeDetailComplement
            val newEpisodes = listOf(mockk<Episode>(relaxed = true))
            val episodesResponse = EpisodesResponse(totalEpisodes = 1, episodes = newEpisodes)
            coEvery {
                TimeUtils.isEpisodeAreUpToDate(
                    "12:00",
                    "UTC",
                    "Monday",
                    lastEpisodeUpdatedAt
                )
            } returns false
            coEvery { runwayAPI.getEpisodes("anime_123") } returns Response.success(episodesResponse)
            coEvery { ResponseHandler.handleCommonResponse(any<Response<EpisodesResponse>>()) } returns Resource.Success(
                episodesResponse
            )
            coEvery { animeDetailComplementDao.updateAnimeDetailComplement(any()) } returns Unit

            val result = repository.updateCachedAnimeDetailComplementWithEpisodes(
                animeDetail,
                cachedAnimeDetailComplement
            )
            testDispatcher.scheduler.advanceUntilIdle()

            Assert.assertNotNull(result)
            coVerify { runwayAPI.getEpisodes("anime_123") }
            coVerify { animeDetailComplementDao.updateAnimeDetailComplement(any()) }
        }

    @Test
    fun `updateCachedAnimeDetailComplementWithEpisodes returns cached data when not needed`() =
        runTest {
            val animeId = 123
            val lastEpisodeUpdatedAt = Instant.now().epochSecond
            val broadcast = mockk<Broadcast>(relaxed = true) {
                every { time } returns "12:00"
                every { timezone } returns "UTC"
                every { day } returns "Monday"
            }
            val animeDetail = mockk<AnimeDetail>(relaxed = true) {
                every { mal_id } returns animeId
                every { airing } returns true
                every { this@mockk.broadcast } returns broadcast
            }
            val cachedAnimeDetailComplement = mockk<AnimeDetailComplement>(relaxed = true) {
                every { id } returns "anime_123"
                every { this@mockk.lastEpisodeUpdatedAt } returns lastEpisodeUpdatedAt
            }
            coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(animeId) } returns cachedAnimeDetailComplement
            coEvery {
                TimeUtils.isEpisodeAreUpToDate(
                    "12:00",
                    "UTC",
                    "Monday",
                    lastEpisodeUpdatedAt
                )
            } returns true

            val result = repository.updateCachedAnimeDetailComplementWithEpisodes(
                animeDetail,
                cachedAnimeDetailComplement
            )
            testDispatcher.scheduler.advanceUntilIdle()

            Assert.assertEquals(cachedAnimeDetailComplement, result)
            coVerify(exactly = 0) { runwayAPI.getEpisodes(any()) }
        }

    @Test
    fun `updateCachedAnimeDetailComplementWithEpisodes returns cached data when api call fails`() =
        runTest {
            val animeId = 123
            val lastEpisodeUpdatedAt = Instant.now().epochSecond - 3600
            val broadcast = mockk<Broadcast>(relaxed = true) {
                every { time } returns "12:00"
                every { timezone } returns "UTC"
                every { day } returns "Monday"
            }
            val animeDetail = mockk<AnimeDetail>(relaxed = true) {
                every { mal_id } returns animeId
                every { airing } returns true
                every { this@mockk.broadcast } returns broadcast
            }
            val cachedAnimeDetailComplement = mockk<AnimeDetailComplement>(relaxed = true) {
                every { id } returns "anime_123"
                every { this@mockk.lastEpisodeUpdatedAt } returns lastEpisodeUpdatedAt
            }
            coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(animeId) } returns cachedAnimeDetailComplement
            coEvery {
                TimeUtils.isEpisodeAreUpToDate(
                    "12:00",
                    "UTC",
                    "Monday",
                    lastEpisodeUpdatedAt
                )
            } returns false
            coEvery { runwayAPI.getEpisodes("anime_123") } returns Response.error(
                500,
                "Server error".toResponseBody()
            )
            coEvery { ResponseHandler.handleCommonResponse(any<Response<EpisodesResponse>>()) } returns Resource.Error(
                "Server error"
            )

            val result = repository.updateCachedAnimeDetailComplementWithEpisodes(
                animeDetail,
                cachedAnimeDetailComplement
            )
            testDispatcher.scheduler.advanceUntilIdle()

            Assert.assertEquals(cachedAnimeDetailComplement, result)
            coVerify { runwayAPI.getEpisodes("anime_123") }
        }

    @Test
    fun `getEpisodes returns success with valid response`() = runTest {
        val id = "anime_123"
        val episodesResponse = mockk<EpisodesResponse>(relaxed = true)
        coEvery { runwayAPI.getEpisodes(id) } returns Response.success(episodesResponse)
        coEvery { ResponseHandler.handleCommonResponse(any<Response<EpisodesResponse>>()) } returns Resource.Success(
            episodesResponse
        )

        val result = repository.getEpisodes(id)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Success) {
            println("Unexpected result: $result")
            if (result is Resource.Error) {
                println("Error message: ${result.message}")
            }
        }
        Assert.assertTrue(result is Resource.Success)
        Assert.assertEquals(episodesResponse, (result as Resource.Success).data)
        coVerify { runwayAPI.getEpisodes(id) }
    }

    @Test
    fun `getEpisodes returns error when api call fails`() = runTest {
        val id = "anime_123"
        coEvery { runwayAPI.getEpisodes(id) } returns Response.error(
            500,
            "Server error".toResponseBody()
        )
        coEvery { ResponseHandler.handleCommonResponse(any<Response<EpisodesResponse>>()) } returns Resource.Error(
            "Server error"
        )

        val result = repository.getEpisodes(id)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Error) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Error)
        Assert.assertEquals("Server error", (result as Resource.Error).message)
        coVerify { runwayAPI.getEpisodes(id) }
    }

    @Test
    fun `getEpisodeServers returns success with valid response`() = runTest {
        val episodeId = "episode_123"
        val serversResponse = mockk<EpisodeServersResponse>(relaxed = true)
        coEvery { runwayAPI.getEpisodeServers(episodeId) } returns Response.success(serversResponse)
        coEvery { ResponseHandler.handleCommonResponse(any<Response<EpisodeServersResponse>>()) } returns Resource.Success(
            serversResponse
        )

        val result = repository.getEpisodeServers(episodeId)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Success) {
            println("Unexpected result: $result")
            if (result is Resource.Error) {
                println("Error message: ${result.message}")
            }
        }
        Assert.assertTrue(result is Resource.Success)
        Assert.assertEquals(serversResponse, (result as Resource.Success).data)
        coVerify { runwayAPI.getEpisodeServers(episodeId) }
    }

    @Test
    fun `getEpisodeServers returns error when api call fails`() = runTest {
        val episodeId = "episode_123"
        coEvery { runwayAPI.getEpisodeServers(episodeId) } returns Response.error(
            500,
            "Server error".toResponseBody()
        )
        coEvery { ResponseHandler.handleCommonResponse(any<Response<EpisodeServersResponse>>()) } returns Resource.Error(
            "Server error"
        )

        val result = repository.getEpisodeServers(episodeId)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Error) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Error)
        Assert.assertEquals("Server error", (result as Resource.Error).message)
        coVerify { runwayAPI.getEpisodeServers(episodeId) }
    }

    @Test
    fun `getEpisodeSources returns success with valid response`() = runTest {
        val episodeId = "episode_123"
        val server = "server1"
        val category = "category1"
        val sourcesResponse = mockk<EpisodeSourcesResponse>(relaxed = true)
        coEvery {
            runwayAPI.getEpisodeSources(
                episodeId,
                server,
                category
            )
        } returns Response.success(sourcesResponse)

        val result = repository.getEpisodeSources(episodeId, server, category)
        testDispatcher.scheduler.advanceUntilIdle()

        if (!result.isSuccessful) {
            println("Unexpected result: $result")
            println("Error body: ${result.errorBody()?.string()}")
        }
        Assert.assertTrue(result.isSuccessful)
        Assert.assertEquals(sourcesResponse, result.body())
        coVerify { runwayAPI.getEpisodeSources(episodeId, server, category) }
    }

    @Test
    fun `getEpisodeSources returns error when api call fails`() = runTest {
        val episodeId = "episode_123"
        val server = "server1"
        val category = "category1"
        coEvery { runwayAPI.getEpisodeSources(episodeId, server, category) } returns Response.error(
            500,
            "Server error".toResponseBody()
        )

        val result = repository.getEpisodeSources(episodeId, server, category)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result.isSuccessful) {
            println("Unexpected result: $result")
        }
        Assert.assertFalse(result.isSuccessful)
        Assert.assertEquals("Server error", result.errorBody()?.string())
        coVerify { runwayAPI.getEpisodeSources(episodeId, server, category) }
    }

    @Test
    fun `getCachedAnimeDetailComplementByMalId returns cached data`() = runTest {
        val malId = 123
        val animeDetailComplement = mockk<AnimeDetailComplement>(relaxed = true)
        coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(malId) } returns animeDetailComplement

        val result = repository.getCachedAnimeDetailComplementByMalId(malId)
        testDispatcher.scheduler.advanceUntilIdle()

        Assert.assertEquals(animeDetailComplement, result)
        coVerify { animeDetailComplementDao.getAnimeDetailComplementByMalId(malId) }
    }

    @Test
    fun `insertCachedAnimeDetailComplement inserts data`() = runTest {
        val animeDetailComplement = mockk<AnimeDetailComplement>(relaxed = true)
        coEvery { animeDetailComplementDao.insertAnimeDetailComplement(animeDetailComplement) } returns Unit

        repository.insertCachedAnimeDetailComplement(animeDetailComplement)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { animeDetailComplementDao.insertAnimeDetailComplement(animeDetailComplement) }
    }

    @Test
    fun `getCachedEpisodeDetailComplement returns cached data`() = runTest {
        val episodeId = "episode_123"
        val episodeDetailComplement = mockk<EpisodeDetailComplement>(relaxed = true)
        coEvery { episodeDetailComplementDao.getEpisodeDetailComplementById(episodeId) } returns episodeDetailComplement

        val result = repository.getCachedEpisodeDetailComplement(episodeId)
        testDispatcher.scheduler.advanceUntilIdle()

        Assert.assertEquals(episodeDetailComplement, result)
        coVerify { episodeDetailComplementDao.getEpisodeDetailComplementById(episodeId) }
    }

    @Test
    fun `insertCachedEpisodeDetailComplement inserts data`() = runTest {
        val episodeDetailComplement = mockk<EpisodeDetailComplement>(relaxed = true)
        coEvery { episodeDetailComplementDao.insertEpisodeDetailComplement(episodeDetailComplement) } returns Unit

        repository.insertCachedEpisodeDetailComplement(episodeDetailComplement)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { episodeDetailComplementDao.insertEpisodeDetailComplement(episodeDetailComplement) }
    }

    @Test
    fun `updateEpisodeDetailComplement updates data`() = runTest {
        val episodeDetailComplement = mockk<EpisodeDetailComplement>(relaxed = true)
        coEvery { episodeDetailComplementDao.updateEpisodeDetailComplement(any()) } returns Unit

        repository.updateEpisodeDetailComplement(episodeDetailComplement)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { episodeDetailComplementDao.updateEpisodeDetailComplement(any()) }
    }

    @Test
    fun `getAnimeAniwatchSearch returns success with valid response`() = runTest {
        val keyword = "naruto"
        val searchResponse = mockk<AnimeAniwatchSearchResponse>(relaxed = true)
        coEvery { runwayAPI.getAnimeAniwatchSearch(keyword) } returns Response.success(
            searchResponse
        )
        coEvery { ResponseHandler.handleCommonResponse<AnimeAniwatchSearchResponse>(any()) } returns Resource.Success(
            searchResponse
        )

        val result = repository.getAnimeAniwatchSearch(keyword)
        testDispatcher.scheduler.advanceUntilIdle()

        if (!result.isSuccessful) {
            println("Unexpected result: $result")
            println("Error body: ${result.errorBody()?.string()}")
        }
        Assert.assertTrue(result.isSuccessful)
        Assert.assertEquals(searchResponse, result.body())
        coVerify { runwayAPI.getAnimeAniwatchSearch(keyword) }
    }

    @Test
    fun `getAnimeAniwatchSearch returns error when api call fails`() = runTest {
        val keyword = "naruto"
        coEvery { runwayAPI.getAnimeAniwatchSearch(keyword) } returns Response.error(
            500,
            "Server error".toResponseBody()
        )
        coEvery { ResponseHandler.handleCommonResponse<AnimeAniwatchSearchResponse>(any()) } returns Resource.Error(
            "Server error"
        )

        val result = repository.getAnimeAniwatchSearch(keyword)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result.isSuccessful) {
            println("Unexpected result: $result")
        }
        Assert.assertFalse(result.isSuccessful)
        Assert.assertEquals("Server error", result.errorBody()?.string())
        coVerify { runwayAPI.getAnimeAniwatchSearch(keyword) }
    }
}