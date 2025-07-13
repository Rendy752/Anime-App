package com.luminoverse.animevibe.repository

import com.luminoverse.animevibe.data.local.dao.AnimeDetailDao
import com.luminoverse.animevibe.data.local.dao.AnimeDetailComplementDao
import com.luminoverse.animevibe.data.local.dao.EpisodeDetailComplementDao
import com.luminoverse.animevibe.data.remote.api.AnimeAPI
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.ui.common.AnimeAniwatchCommonResponse
import com.luminoverse.animevibe.utils.ComplementUtils
import com.luminoverse.animevibe.utils.media.StreamingUtils
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.watch.AnimeTitleFinder
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeEpisodeDetailRepositoryTest {

    private val animeDetailDao: AnimeDetailDao = mockk(relaxed = true)
    private val animeDetailComplementDao: AnimeDetailComplementDao = mockk(relaxed = true)
    private val episodeDetailComplementDao: EpisodeDetailComplementDao = mockk(relaxed = true)
    private val jikanAPI: AnimeAPI = mockk(relaxed = true)
    private val runwayAPI: AnimeAPI = mockk(relaxed = true)
    private lateinit var repository: AnimeEpisodeDetailRepository
    private val testDispatcher = StandardTestDispatcher()

    private val mockAnimeDetail =
        animeDetailPlaceholder.copy(mal_id = 1, type = "TV", airing = true)
    private val mockAnimeResponse = AnimeDetailResponse(mockAnimeDetail)
    private val mockEpisode1 = episodePlaceholder.copy(id = "ep1", episode_no = 1)
    private val mockEpisode2 = episodePlaceholder.copy(id = "ep2", episode_no = 2)
    private val mockAnimeComplement =
        animeDetailComplementPlaceholder.copy(malId = 1, episodes = listOf(mockEpisode1))
    private val mockEpisodeComplement =
        episodeDetailComplementPlaceholder.copy(id = "ep1", malId = 1)
    private val mockAniwatchSearchResponse = AnimeAniwatchCommonResponse(
        true,
        AnimeAniwatchSearchResponse(
            listOf(
                animeAniwatchPlaceholder.copy(
                    malID = 1,
                    id = "aniwatch-1"
                )
            ), 1
        )
    )
    private val mockEpisodesResponse =
        AnimeAniwatchCommonResponse(true, EpisodesResponse(1, listOf(mockEpisode1)))
    private val mockServersResponse =
        AnimeAniwatchCommonResponse(true, listOf(episodeServerPlaceholder))
    private val mockSourcesResponse = AnimeAniwatchCommonResponse(
        true,
        EpisodeSourcesResponse(episodeSourcesPlaceholder, emptyList())
    )


    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = AnimeEpisodeDetailRepository(
            animeDetailDao,
            animeDetailComplementDao,
            episodeDetailComplementDao,
            jikanAPI,
            runwayAPI
        )
        mockkObject(ComplementUtils)
        mockkObject(StreamingUtils)
        mockkObject(AnimeTitleFinder)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `getAnimeDetail returns cached data when available and not airing`() = runTest {
        val animeId = 1
        val cachedDetail = mockAnimeDetail.copy(airing = false)
        coEvery { animeDetailDao.getAnimeDetailById(animeId) } returns cachedDetail

        val (result, isCached) = repository.getAnimeDetail(animeId)

        assertTrue(result is Resource.Success)
        assertTrue(isCached)
        assertEquals(cachedDetail, result.data?.data)
        coVerify(exactly = 0) { jikanAPI.getAnimeDetail(any()) }
    }

    @Test
    fun `getAnimeDetail fetches remote data when cache is not available`() = runTest {
        val animeId = 1
        coEvery { animeDetailDao.getAnimeDetailById(animeId) } returns null
        coEvery { jikanAPI.getAnimeDetail(animeId) } returns Response.success(mockAnimeResponse)

        repository.getAnimeDetail(animeId)

        coVerify(exactly = 1) { animeDetailDao.insertAnimeDetail(mockAnimeDetail) }
    }

    @Test
    fun `loadAllEpisodes returns error when search fails and no cache exists`() = runTest {
        coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(1) } returns null
        coEvery { runwayAPI.getAnimeAniwatchSearch(any()) } returns Response.error(
            404,
            mockk(relaxed = true)
        )

        val result = repository.loadAllEpisodes(mockAnimeDetail, isRefresh = false)

        assertTrue(result is LoadEpisodesResult.Error)
    }

    @Test
    fun `loadAllEpisodes returns success with no new episodes if refresh finds none`() = runTest {
        val existingComplement = mockAnimeComplement.copy(
            episodes = listOf(mockEpisode1, mockEpisode2),
            id = "aniwatch-1"
        )
        coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(1) } returns existingComplement
        coEvery {
            ComplementUtils.updateAnimeDetailComplementWithEpisodes(
                any(),
                any(),
                any(),
                any()
            )
        } returns existingComplement

        val result = repository.loadAllEpisodes(mockAnimeDetail, isRefresh = true)

        assertTrue(result is LoadEpisodesResult.Success)
        val successResult = result as LoadEpisodesResult.Success
        assertTrue("New episode list should be empty", successResult.newEpisodeIds.isEmpty())
    }

    @Test
    fun `getEpisodeStreamingDetails returns cached data if query matches`() = runTest {
        val query = mockEpisodeComplement.sourcesQuery
        coEvery { episodeDetailComplementDao.getEpisodeDetailComplementById(query.id) } returns mockEpisodeComplement

        val result = repository.getEpisodeStreamingDetails(query, false, emptyList(), null, null)

        assertTrue(result is Resource.Success)
        assertEquals(mockEpisodeComplement, result.data)
        coVerify(exactly = 0) { runwayAPI.getEpisodeServers(any(), any()) }
    }

    @Test
    fun `getEpisodeStreamingDetails returns error when details are missing`() = runTest {
        val query = episodeSourcesQueryPlaceholder.copy(id = "ep1")
        coEvery { episodeDetailComplementDao.getEpisodeDetailComplementById(any()) } returns null

        val result = repository.getEpisodeStreamingDetails(
            query,
            false,
            emptyList(),
            null,
            mockAnimeComplement
        )

        assertTrue(result is Resource.Error)
        assertEquals("Anime details not loaded.", result.message)
    }

    @Test
    fun `getEpisodeStreamingDetails returns error when episode info is not in complement`() = runTest {
        val query = episodeSourcesQueryPlaceholder.copy(id = "ep_that_does_not_exist")
        val animeComplementWithEpisode = mockAnimeComplement.copy(episodes = listOf(mockEpisode1))
        coEvery { episodeDetailComplementDao.getEpisodeDetailComplementById(any()) } returns null
        coEvery { runwayAPI.getEpisodeServers(any(), any()) } returns Response.success(mockServersResponse)
        coEvery { StreamingUtils.getEpisodeSourcesResult(any(), any(), any(), any(), any(), any()) } returns Pair(Resource.Success(mockSourcesResponse), query)

        val result = repository.getEpisodeStreamingDetails(query, false, emptyList(), mockAnimeDetail, animeComplementWithEpisode)

        assertTrue(result is Resource.Error)
        assertEquals("Episode info not found in complement.", result.message)
    }

    @Test
    fun `toggleEpisodeFavorite returns error when episode not found`() = runTest {
        val episodeId = "non_existent_ep"
        coEvery { episodeDetailComplementDao.getEpisodeDetailComplementById(episodeId) } returns null

        val result = repository.toggleEpisodeFavorite(episodeId, true)

        assertTrue(result is Resource.Error)
        assertEquals("Episode not found", result.message)
        coVerify(exactly = 0) { episodeDetailComplementDao.updateEpisodeDetailComplement(any()) }
    }

    @Test
    fun `loadAllEpisodes with no cache searches, finds, and fetches episodes`() = runTest {
        coEvery { animeDetailComplementDao.getAnimeDetailComplementByMalId(1) } returns null
        coEvery { runwayAPI.getAnimeAniwatchSearch(any()) } returns Response.success(
            mockAniwatchSearchResponse
        )
        coEvery { runwayAPI.getEpisodes("aniwatch-1") } returns Response.success(
            mockEpisodesResponse
        )
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(
                any(),
                any(),
                any()
            )
        } returns mockAnimeComplement

        val result = repository.loadAllEpisodes(mockAnimeDetail, isRefresh = false)

        assertTrue(result is LoadEpisodesResult.Success)
        val successResult = result as LoadEpisodesResult.Success
        assertEquals(1, successResult.complement.episodes?.size)
        coVerify { runwayAPI.getAnimeAniwatchSearch(any()) }
        coVerify { runwayAPI.getEpisodes("aniwatch-1") }
    }

    @Test
    fun `getEpisodeStreamingDetails fetches remote data when no cache`() = runTest {
        val query = episodeSourcesQueryPlaceholder.copy(id = "ep1")
        val animeComplementWithEpisode = mockAnimeComplement.copy(episodes = listOf(mockEpisode1))

        coEvery { episodeDetailComplementDao.getEpisodeDetailComplementById(query.id) } returns null
        coEvery { runwayAPI.getEpisodeServers(any(), any()) } returns Response.success(
            mockServersResponse
        )
        coEvery {
            StreamingUtils.getEpisodeSourcesResult(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Pair(Resource.Success(mockSourcesResponse), query)

        val result = repository.getEpisodeStreamingDetails(
            query,
            false,
            emptyList(),
            mockAnimeDetail,
            animeComplementWithEpisode
        )

        assertTrue(result is Resource.Success)
        coVerify { runwayAPI.getEpisodeServers(any(), any()) }
        coVerify { episodeDetailComplementDao.insertEpisodeDetailComplement(any()) }
    }

    @Test
    fun `getPaginatedAndFilteredHistory returns paginated data`() = runTest {
        val query = EpisodeHistoryQueryState(page = 1, limit = 5)
        val fullList = listOf(mockEpisodeComplement)
        coEvery { episodeDetailComplementDao.getAllEpisodeHistory(any(), any()) } returns fullList
        every {
            AnimeTitleFinder.searchTitle<EpisodeDetailComplement>(
                any(),
                any(),
                any()
            )
        } returns fullList
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(
                repository,
                malId = 1
            )
        } returns mockAnimeComplement

        val result = repository.getPaginatedAndFilteredHistory(query)

        assertTrue(result is Resource.Success)
        val historyResult = result.data!!
        assertEquals(1, historyResult.data.size)
        assertEquals(1, historyResult.pagination.items.total)
    }
}