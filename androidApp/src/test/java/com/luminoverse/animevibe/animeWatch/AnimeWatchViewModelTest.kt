package com.luminoverse.animevibe.animeWatch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.repository.LoadEpisodesResult
import com.luminoverse.animevibe.ui.animeWatch.AnimeWatchViewModel
import com.luminoverse.animevibe.ui.animeWatch.WatchAction
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.resource.Resource
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@ExperimentalCoroutinesApi
class AnimeWatchViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AnimeWatchViewModel
    private lateinit var repository: AnimeEpisodeDetailRepository
    private lateinit var hlsPlayerUtils: HlsPlayerUtils
    private lateinit var networkDataSource: NetworkDataSource
    private lateinit var testDispatcher: TestDispatcher

    private val mockAnimeDetail = animeDetailPlaceholder.copy(mal_id = 1)
    private val mockAnimeResponse = AnimeDetailResponse(data = mockAnimeDetail)
    private val mockAnimeComplement = animeDetailComplementPlaceholder.copy(malId = 1, episodes = listOf(episodePlaceholder))
    private val mockEpisodeComplement = episodeDetailComplementPlaceholder.copy(malId = 1, id = episodePlaceholder.id)

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        hlsPlayerUtils = mockk(relaxed = true) {
            every { playerCoreState } returns MutableStateFlow(PlayerCoreState())
            every { controlsState } returns MutableStateFlow(ControlsState())
        }
        networkDataSource = mockk(relaxed = true)

        viewModel = AnimeWatchViewModel(repository, hlsPlayerUtils, networkDataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `SetInitialState success flow updates all detail states correctly`() = runTest {
        val malId = 1
        val episodeId = episodePlaceholder.id

        coEvery { repository.getAnimeDetail(malId) } returns Pair(Resource.Success(mockAnimeResponse), false)
        coEvery { repository.loadAllEpisodes(any(), any()) } returns LoadEpisodesResult.Success(mockAnimeComplement)
        coEvery { repository.getCachedEpisodeDetailComplement(episodeId) } returns mockEpisodeComplement
        coEvery { repository.getEpisodeStreamingDetails(any(), any(), any(), any(), any()) } returns Resource.Success(mockEpisodeComplement)

        viewModel.onAction(WatchAction.SetInitialState(malId, episodeId))
        advanceUntilIdle()

        with(viewModel.watchState.value) {
            assertTrue("AnimeDetail should be Success", animeDetail is Resource.Success)
            assertEquals(mockAnimeDetail, animeDetail.data)

            assertTrue("AnimeDetailComplement should be Success", animeDetailComplement is Resource.Success)
            assertEquals(mockAnimeComplement, animeDetailComplement.data)

            assertTrue("EpisodeDetailComplement should be Success", episodeDetailComplement is Resource.Success)
            assertEquals(mockEpisodeComplement, episodeDetailComplement.data)
        }

        coVerifyOrder {
            repository.getAnimeDetail(malId)
            repository.loadAllEpisodes(mockAnimeDetail, true)
            repository.getEpisodeStreamingDetails(any(), false, any(), any(), any())
        }
    }

    @Test
    fun `HandleSelectedEpisodeServer success should update episodeDetailComplement`() = runTest {
        val query = episodeSourcesQueryPlaceholder
        coEvery { repository.getEpisodeStreamingDetails(any(), any(), any(), any(), any()) } returns Resource.Success(mockEpisodeComplement)

        viewModel.onAction(WatchAction.HandleSelectedEpisodeServer(query))
        advanceUntilIdle()

        with(viewModel.watchState.value) {
            assertTrue(episodeDetailComplement is Resource.Success)
            assertEquals(mockEpisodeComplement, episodeDetailComplement.data)
            assertEquals(mockEpisodeComplement.sourcesQuery, episodeSourcesQuery)
        }
        coVerify(exactly = 1) { repository.getEpisodeStreamingDetails(query, false, any(), any(), any()) }
    }

    @Test
    fun `HandleSelectedEpisodeServer error should update state with error`() = runTest {
        val query = episodeSourcesQueryPlaceholder
        val errorMessage = "Failed to load streams"
        coEvery { repository.getEpisodeStreamingDetails(any(), any(), any(), any(), any()) } returns Resource.Error(errorMessage)

        viewModel.onAction(WatchAction.HandleSelectedEpisodeServer(query))
        advanceUntilIdle()

        with(viewModel.watchState.value) {
            assertTrue(episodeDetailComplement is Resource.Error)
            assertEquals(errorMessage, episodeDetailComplement.message)
        }
    }

    @Test
    fun `UpdateStoredWatchState should call repository to insert or update`() = runTest {
        val position = 100L
        val duration = 200L
        coEvery { repository.getCachedEpisodeDetailComplement(any()) } returns mockEpisodeComplement
        coEvery { repository.getEpisodeStreamingDetails(any(), any(), any(), any(), any()) } returns Resource.Success(mockEpisodeComplement)
        viewModel.onAction(WatchAction.HandleSelectedEpisodeServer(episodeSourcesQueryPlaceholder))
        advanceUntilIdle()

        viewModel.onAction(WatchAction.UpdateStoredWatchState(position, duration, null))
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.insertCachedEpisodeDetailComplement(withArg {
                assertEquals(position, it.lastTimestamp)
                assertEquals(duration, it.duration)
            })
        }
    }

    @Test
    fun `LoadEpisodeDetailComplement with cached data should update map with success`() = runTest {
        val episodeId = "ep1"
        coEvery { repository.getCachedEpisodeDetailComplement(episodeId) } returns mockEpisodeComplement

        viewModel.onAction(WatchAction.LoadEpisodeDetailComplement(episodeId))
        advanceUntilIdle()

        val result = viewModel.watchState.value.episodeDetailComplements[episodeId]
        assertTrue(result is Resource.Success)
        assertEquals(mockEpisodeComplement, result?.data)
    }

    @Test
    fun `LoadEpisodeDetailComplement with no cached data should update map with error`() = runTest {
        val episodeId = "ep1"
        coEvery { repository.getCachedEpisodeDetailComplement(episodeId) } returns null

        viewModel.onAction(WatchAction.LoadEpisodeDetailComplement(episodeId))
        advanceUntilIdle()

        val result = viewModel.watchState.value.episodeDetailComplements[episodeId]
        assertTrue(result is Resource.Error)
        assertEquals("Episode detail complement not found", result?.message)
    }
}