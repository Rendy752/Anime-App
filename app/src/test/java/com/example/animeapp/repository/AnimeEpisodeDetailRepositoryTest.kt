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
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

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
        val broadcast = mockk<Broadcast> {
            every { time } returns "12:00"
            every { timezone } returns "Asia/Tokyo"
            every { day } returns "Monday"
        }
        val animeDetail = mockk<AnimeDetail> {
            every { mal_id } returns animeId
            every { airing } returns false
            every { this@mockk.broadcast } returns broadcast
        }
        val animeDetailResponse = AnimeDetailResponse(animeDetail)
        val animeDetailComplement = mockk<AnimeDetailComplement> {
            every { this@mockk.lastEpisodeUpdatedAt } returns lastEpisodeUpdatedAt
        }
        coEvery { animeDetailDao.getAnimeDetailById(animeId) } returns animeDetail
        coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(animeId) } returns animeDetailComplement
        coEvery {
            TimeUtils.isEpisodeAreUpToDate("12:00", "Asia/Tokyo", "Monday", lastEpisodeUpdatedAt)
        } returns true

        val result = repository.getAnimeDetail(animeId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result is Resource.Success)
        assertEquals(animeDetailResponse, (result as Resource.Success).data)
        coVerify { animeDetailDao.getAnimeDetailById(animeId) }
        coVerify { animeDetailComplementDao.getAnimeDetailComplementByMalId(animeId) }
        coVerify(exactly = 0) { jikanAPI.getAnimeDetail(any()) }
    }

    @Test
    fun `getAnimeDetail returns remote data when cache is not available`() = runTest {
        val animeId = 123
        val animeDetail = mockk<AnimeDetail> {
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

        assertTrue(result is Resource.Success)
        assertEquals(animeDetailResponse, (result as Resource.Success).data)
        coVerify { animeDetailDao.getAnimeDetailById(animeId) }
        coVerify { jikanAPI.getAnimeDetail(animeId) }
        coVerify { animeDetailDao.insertAnimeDetail(animeDetail) }
    }

    @Test
    fun `getAnimeDetail returns remote data when cache is outdated`() = runTest {
        val animeId = 1735
        val lastEpisodeUpdatedAt = Instant.now().epochSecond - 3600
        val broadcast = mockk<Broadcast> {
            every { time } returns "12:00"
            every { timezone } returns "Asia/Tokyo"
            every { day } returns "Monday"
        }
        val animeDetail = mockk<AnimeDetail> {
            every { mal_id } returns animeId
            every { airing } returns true
            every { this@mockk.broadcast } returns broadcast
        }
        val newAnimeDetail = mockk<AnimeDetail> {
            every { mal_id } returns animeId
        }
        val animeDetailResponse = AnimeDetailResponse(newAnimeDetail)
        val animeDetailComplement = mockk<AnimeDetailComplement> {
            every { this@mockk.lastEpisodeUpdatedAt } returns lastEpisodeUpdatedAt
        }
        coEvery { animeDetailDao.getAnimeDetailById(animeId) } returns animeDetail
        coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(animeId) } returns animeDetailComplement
        coEvery {
            TimeUtils.isEpisodeAreUpToDate("12:00", "Asia/Tokyo", "Monday", lastEpisodeUpdatedAt)
        } returns true
        coEvery { jikanAPI.getAnimeDetail(animeId) } returns Response.success(animeDetailResponse)
        coEvery { ResponseHandler.handleCommonResponse(any<Response<AnimeDetailResponse>>()) } returns Resource.Success(
            animeDetailResponse
        )
        coEvery { animeDetailDao.insertAnimeDetail(newAnimeDetail) } returns Unit

        val result = repository.getAnimeDetail(animeId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result is Resource.Success)
        assertEquals(animeDetailResponse, (result as Resource.Success).data)
        coVerify { animeDetailDao.getAnimeDetailById(animeId) }
        coVerify { animeDetailComplementDao.getAnimeDetailComplementByMalId(animeId) }
        coVerify { jikanAPI.getAnimeDetail(animeId) }
        coVerify { animeDetailDao.insertAnimeDetail(newAnimeDetail) }
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

        assertTrue(result is Resource.Error)
        assertEquals("Server error", (result as Resource.Error).message)
        coVerify { animeDetailDao.getAnimeDetailById(animeId) }
        coVerify { jikanAPI.getAnimeDetail(animeId) }
    }

    @Test
    fun `getCachedAnimeDetailById returns cached data`() = runTest {
        val animeId = 123
        val animeDetail = mockk<AnimeDetail>()
        coEvery { animeDetailDao.getAnimeDetailById(animeId) } returns animeDetail

        val result = repository.getCachedAnimeDetailById(animeId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(animeDetail, result)
        coVerify { animeDetailDao.getAnimeDetailById(animeId) }
    }

    @Test
    fun `getCachedAnimeDetailComplementByMalId returns cached data`() = runTest {
        val malId = 123
        val animeDetailComplement = mockk<AnimeDetailComplement>()
        coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(malId) } returns animeDetailComplement

        val result = repository.getCachedAnimeDetailComplementByMalId(malId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(animeDetailComplement, result)
        coVerify { animeDetailComplementDao.getAnimeDetailComplementByMalId(malId) }
    }

    @Test
    fun `insertCachedAnimeDetailComplement inserts data`() = runTest {
        val animeDetailComplement = mockk<AnimeDetailComplement>()
        coEvery { animeDetailComplementDao.insertAnimeDetailComplement(animeDetailComplement) } returns Unit

        repository.insertCachedAnimeDetailComplement(animeDetailComplement)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { animeDetailComplementDao.insertAnimeDetailComplement(animeDetailComplement) }
    }

    @Test
    fun `updateCachedAnimeDetailComplement updates data with timestamp`() = runTest {
        val animeDetailComplement = mockk<AnimeDetailComplement>()
        val updatedComplement = mockk<AnimeDetailComplement>()
        val capturedComplement = slot<AnimeDetailComplement>()
        coEvery { animeDetailComplement.copy(updatedAt = any()) } returns updatedComplement
        coEvery { animeDetailComplementDao.updateAnimeDetailComplement(capture(capturedComplement)) } returns Unit
        every { updatedComplement.updatedAt } returns Instant.now().epochSecond

        repository.updateCachedAnimeDetailComplement(animeDetailComplement)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(capturedComplement.captured.updatedAt > 0)
        assertEquals(updatedComplement, capturedComplement.captured)
        coVerify { animeDetailComplementDao.updateAnimeDetailComplement(updatedComplement) }
    }

    @Test
    fun `getCachedLatestWatchedEpisodeDetailComplement returns cached data`() = runTest {
        val episodeDetailComplement = mockk<EpisodeDetailComplement>()
        coEvery { episodeDetailComplementDao.getLatestWatchedEpisodeDetailComplement() } returns episodeDetailComplement

        val result = repository.getCachedLatestWatchedEpisodeDetailComplement()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(episodeDetailComplement, result)
        coVerify { episodeDetailComplementDao.getLatestWatchedEpisodeDetailComplement() }
    }

    @Test
    fun `getCachedDefaultEpisodeDetailComplementByMalId returns cached data`() = runTest {
        val malId = 123
        val episodeDetailComplement = mockk<EpisodeDetailComplement>()
        coEvery { episodeDetailComplementDao.getDefaultEpisodeDetailComplementByMalId(malId) } returns episodeDetailComplement

        val result = repository.getCachedDefaultEpisodeDetailComplementByMalId(malId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(episodeDetailComplement, result)
        coVerify { episodeDetailComplementDao.getDefaultEpisodeDetailComplementByMalId(malId) }
    }

    @Test
    fun `getCachedEpisodeDetailComplement returns cached data`() = runTest {
        val episodeId = "episode_123"
        val episodeDetailComplement = mockk<EpisodeDetailComplement>()
        coEvery { episodeDetailComplementDao.getEpisodeDetailComplementById(episodeId) } returns episodeDetailComplement

        val result = repository.getCachedEpisodeDetailComplement(episodeId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(episodeDetailComplement, result)
        coVerify { episodeDetailComplementDao.getEpisodeDetailComplementById(episodeId) }
    }

    @Test
    fun `insertCachedEpisodeDetailComplement inserts data`() = runTest {
        val episodeDetailComplement = mockk<EpisodeDetailComplement>()
        coEvery { episodeDetailComplementDao.insertEpisodeDetailComplement(episodeDetailComplement) } returns Unit

        repository.insertCachedEpisodeDetailComplement(episodeDetailComplement)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { episodeDetailComplementDao.insertEpisodeDetailComplement(episodeDetailComplement) }
    }

    @Test
    fun `updateEpisodeDetailComplement updates data with timestamp`() = runTest {
        val episodeDetailComplement = mockk<EpisodeDetailComplement>()
        val updatedComplement = mockk<EpisodeDetailComplement>()
        val capturedComplement = slot<EpisodeDetailComplement>()
        coEvery { episodeDetailComplement.copy(updatedAt = any()) } returns updatedComplement
        coEvery {
            episodeDetailComplementDao.updateEpisodeDetailComplement(
                capture(
                    capturedComplement
                )
            )
        } returns Unit
        every { updatedComplement.updatedAt } returns Instant.now().epochSecond

        repository.updateEpisodeDetailComplement(episodeDetailComplement)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(capturedComplement.captured.updatedAt > 0)
        assertEquals(updatedComplement, capturedComplement.captured)
        coVerify { episodeDetailComplementDao.updateEpisodeDetailComplement(updatedComplement) }
    }

    @Test
    fun `getEpisodes returns success with valid response`() = runTest {
        val id = "anime_123"
        val episodesResponse = mockk<EpisodesResponse>()
        coEvery { runwayAPI.getEpisodes(id) } returns Response.success(episodesResponse)
        coEvery { ResponseHandler.handleCommonResponse(any<Response<EpisodesResponse>>()) } returns Resource.Success(
            episodesResponse
        )

        val result = repository.getEpisodes(id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result is Resource.Success)
        assertEquals(episodesResponse, (result as Resource.Success).data)
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

        assertTrue(result is Resource.Error)
        assertEquals("Server error", (result as Resource.Error).message)
        coVerify { runwayAPI.getEpisodes(id) }
    }

    @Test
    fun `getEpisodeServers returns success with valid response`() = runTest {
        val episodeId = "episode_123"
        val serversResponse = mockk<EpisodeServersResponse>()
        coEvery { runwayAPI.getEpisodeServers(episodeId) } returns Response.success(serversResponse)
        coEvery { ResponseHandler.handleCommonResponse(any<Response<EpisodeServersResponse>>()) } returns Resource.Success(
            serversResponse
        )

        val result = repository.getEpisodeServers(episodeId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result is Resource.Success)
        assertEquals(serversResponse, (result as Resource.Success).data)
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

        assertTrue(result is Resource.Error)
        assertEquals("Server error", (result as Resource.Error).message)
        coVerify { runwayAPI.getEpisodeServers(episodeId) }
    }

    @Test
    fun `getAnimeAniwatchSearch returns success with valid response`() = runTest {
        val keyword = "naruto"
        val searchResponse = mockk<AnimeAniwatchSearchResponse>()
        coEvery { runwayAPI.getAnimeAniwatchSearch(keyword) } returns Response.success(
            searchResponse
        )
        coEvery { ResponseHandler.handleCommonResponse(any<Response<AnimeAniwatchSearchResponse>>()) } returns Resource.Success(
            searchResponse
        )

        val result = repository.getAnimeAniwatchSearch(keyword)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result.isSuccessful)
        assertEquals(searchResponse, result.body())
        coVerify { runwayAPI.getAnimeAniwatchSearch(keyword) }
    }

    @Test
    fun `getAnimeAniwatchSearch returns error when api call fails`() = runTest {
        val keyword = "naruto"
        coEvery { runwayAPI.getAnimeAniwatchSearch(keyword) } returns Response.error(
            500,
            "Server error".toResponseBody()
        )
        coEvery { ResponseHandler.handleCommonResponse(any<Response<AnimeAniwatchSearchResponse>>()) } returns Resource.Error(
            "Server error"
        )

        val result = repository.getAnimeAniwatchSearch(keyword)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!result.isSuccessful)
        assertEquals("Server error", result.errorBody()?.string())
        coVerify { runwayAPI.getAnimeAniwatchSearch(keyword) }
    }

    @Test
    fun `getEpisodeSources returns success with valid response`() = runTest {
        val episodeId = "episode_123"
        val server = "server1"
        val category = "category1"
        val sourcesResponse = mockk<EpisodeSourcesResponse>()
        coEvery {
            runwayAPI.getEpisodeSources(
                episodeId,
                server,
                category
            )
        } returns Response.success(sourcesResponse)

        val result = repository.getEpisodeSources(episodeId, server, category)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result.isSuccessful)
        assertEquals(sourcesResponse, result.body())
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

        assertTrue(!result.isSuccessful)
        assertEquals("Server error", result.errorBody()?.string())
        coVerify { runwayAPI.getEpisodeSources(episodeId, server, category) }
    }

    @Test
    fun `getPaginatedEpisodeHistory returns success with valid data`() = runTest {
        val queryState = EpisodeHistoryQueryState(
            searchQuery = "test",
            isFavorite = true,
            sortBy = EpisodeHistoryQueryState.SortBy.NewestFirst,
            page = 1,
            limit = 10
        )
        val episodeDetailComplement = mockk<EpisodeDetailComplement>()
        coEvery {
            episodeDetailComplementDao.getPaginatedEpisodeHistory(
                searchQuery = "test",
                isFavorite = true,
                sortBy = "NewestFirst",
                limit = 10,
                offset = 0
            )
        } returns listOf(episodeDetailComplement)

        val result = repository.getPaginatedEpisodeHistory(queryState)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result is Resource.Success)
        assertEquals(listOf(episodeDetailComplement), (result as Resource.Success).data)
        coVerify {
            episodeDetailComplementDao.getPaginatedEpisodeHistory(
                searchQuery = "test",
                isFavorite = true,
                sortBy = "NewestFirst",
                limit = 10,
                offset = 0
            )
        }
    }

    @Test
    fun `getPaginatedEpisodeHistory returns error when dao throws exception`() = runTest {
        val queryState = EpisodeHistoryQueryState(
            searchQuery = "test",
            isFavorite = true,
            sortBy = EpisodeHistoryQueryState.SortBy.NewestFirst,
            page = 1,
            limit = 10
        )
        coEvery {
            episodeDetailComplementDao.getPaginatedEpisodeHistory(
                searchQuery = "test",
                isFavorite = true,
                sortBy = "NewestFirst",
                limit = 10,
                offset = 0
            )
        } throws RuntimeException("Database error")

        val result = repository.getPaginatedEpisodeHistory(queryState)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result is Resource.Error)
        assertEquals(
            "Failed to fetch episode history: Database error",
            (result as Resource.Error).message
        )
        coVerify {
            episodeDetailComplementDao.getPaginatedEpisodeHistory(
                searchQuery = "test",
                isFavorite = true,
                sortBy = "NewestFirst",
                limit = 10,
                offset = 0
            )
        }
    }

    @Test
    fun `getEpisodeHistoryCount returns success with valid count`() = runTest {
        val searchQuery = "test"
        val isFavorite = true
        coEvery {
            episodeDetailComplementDao.getEpisodeHistoryCount(
                searchQuery,
                isFavorite
            )
        } returns 42

        val result = repository.getEpisodeHistoryCount(searchQuery, isFavorite)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result is Resource.Success)
        assertEquals(42, (result as Resource.Success).data)
        coVerify { episodeDetailComplementDao.getEpisodeHistoryCount(searchQuery, isFavorite) }
    }

    @Test
    fun `getEpisodeHistoryCount returns error when dao throws exception`() = runTest {
        val searchQuery = "test"
        val isFavorite = true
        coEvery {
            episodeDetailComplementDao.getEpisodeHistoryCount(
                searchQuery,
                isFavorite
            )
        } throws RuntimeException("Database error")

        val result = repository.getEpisodeHistoryCount(searchQuery, isFavorite)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result is Resource.Error)
        assertEquals(
            "Failed to fetch episode history count: Database error",
            (result as Resource.Error).message
        )
        coVerify { episodeDetailComplementDao.getEpisodeHistoryCount(searchQuery, isFavorite) }
    }

    @Test
    fun `deleteAnimeDetailComplement returns true when anime exists`() = runTest {
        val malId = 123
        val animeDetailComplement = mockk<AnimeDetailComplement>()
        coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(malId) } returns animeDetailComplement
        coEvery { animeDetailComplementDao.deleteAnimeDetailComplement(animeDetailComplement) } returns Unit
        coEvery { episodeDetailComplementDao.deleteEpisodeDetailComplementByMalId(malId) } returns Unit

        val result = repository.deleteAnimeDetailComplement(malId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result)
        coVerify { animeDetailComplementDao.getAnimeDetailComplementByMalId(malId) }
        coVerify { animeDetailComplementDao.deleteAnimeDetailComplement(animeDetailComplement) }
        coVerify { episodeDetailComplementDao.deleteEpisodeDetailComplementByMalId(malId) }
    }

    @Test
    fun `deleteAnimeDetailComplement returns false when anime does not exist`() = runTest {
        val malId = 123
        coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(malId) } returns null

        val result = repository.deleteAnimeDetailComplement(malId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(result)
        coVerify { animeDetailComplementDao.getAnimeDetailComplementByMalId(malId) }
        coVerify(exactly = 0) { animeDetailComplementDao.deleteAnimeDetailComplement(any()) }
        coVerify(exactly = 0) { episodeDetailComplementDao.deleteEpisodeDetailComplementByMalId(any()) }
    }

    @Test
    fun `deleteEpisodeDetailComplement returns true when episode exists`() = runTest {
        val episodeId = "episode_123"
        val episodeDetailComplement = mockk<EpisodeDetailComplement>()
        coEvery { episodeDetailComplementDao.getEpisodeDetailComplementById(episodeId) } returns episodeDetailComplement
        coEvery { episodeDetailComplementDao.deleteEpisodeDetailComplement(episodeDetailComplement) } returns Unit

        val result = repository.deleteEpisodeDetailComplement(episodeId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result)
        coVerify { episodeDetailComplementDao.getEpisodeDetailComplementById(episodeId) }
        coVerify { episodeDetailComplementDao.deleteEpisodeDetailComplement(episodeDetailComplement) }
    }

    @Test
    fun `deleteEpisodeDetailComplement returns false when episode does not exist`() = runTest {
        val episodeId = "episode_123"
        coEvery { episodeDetailComplementDao.getEpisodeDetailComplementById(episodeId) } returns null

        val result = repository.deleteEpisodeDetailComplement(episodeId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(result)
        coVerify { episodeDetailComplementDao.getEpisodeDetailComplementById(episodeId) }
        coVerify(exactly = 0) { episodeDetailComplementDao.deleteEpisodeDetailComplement(any()) }
    }
}