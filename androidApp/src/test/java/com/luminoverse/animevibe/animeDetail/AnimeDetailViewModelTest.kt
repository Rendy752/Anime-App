package com.luminoverse.animevibe.animeDetail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.repository.LoadEpisodesResult
import com.luminoverse.animevibe.ui.animeDetail.AnimeDetailViewModel
import com.luminoverse.animevibe.ui.animeDetail.DetailAction
import com.luminoverse.animevibe.ui.animeDetail.EpisodeFilterState
import com.luminoverse.animevibe.utils.FilterUtils
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.workers.WorkerScheduler
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@ExperimentalCoroutinesApi
class AnimeDetailViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AnimeDetailViewModel
    private lateinit var repository: AnimeEpisodeDetailRepository
    private val workerScheduler: WorkerScheduler = mockk(relaxed = true)
    private lateinit var testDispatcher: TestDispatcher

    private val mockAnimeDetail = animeDetailPlaceholder.copy(mal_id = 1)
    private val mockAnimeResponse = AnimeDetailResponse(mockAnimeDetail)
    private val mockAnimeComplement = animeDetailComplementPlaceholder.copy(malId = 1)

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        mockkObject(FilterUtils)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkObject(FilterUtils)
    }

    @Test
    fun `loadAnimeDetail success should update state and trigger loadAllEpisodes`() = runTest {
        val animeId = 1
        coEvery { repository.getAnimeDetail(animeId) } returns Pair(Resource.Success(mockAnimeResponse), false)
        coEvery { repository.loadAllEpisodes(any(), any()) } returns LoadEpisodesResult.Success(mockAnimeComplement)
        viewModel = AnimeDetailViewModel(repository, workerScheduler)

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue(state.animeDetail is Resource.Success)
        assertEquals(animeId, state.animeDetail.data?.data?.mal_id)
        assertTrue(state.animeDetailComplement is Resource.Success)
        assertEquals(animeId, state.animeDetailComplement.data?.malId)

        coVerify(exactly = 1) { repository.getAnimeDetail(animeId) }
        coVerify(exactly = 1) { repository.loadAllEpisodes(any(), any()) }
    }

    @Test
    fun `loadAnimeDetail from cache should trigger background update`() = runTest {
        val animeId = 1
        val updatedAnimeResponse = AnimeDetailResponse(mockAnimeDetail.copy(title = "Updated Title"))
        coEvery { repository.getAnimeDetail(animeId) } returns Pair(Resource.Success(mockAnimeResponse), true)
        coEvery { repository.getUpdatedAnimeDetailById(animeId) } returns Resource.Success(updatedAnimeResponse)
        coEvery { repository.loadAllEpisodes(any(), any()) } returns LoadEpisodesResult.Success(mockAnimeComplement)
        viewModel = AnimeDetailViewModel(repository, workerScheduler)

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue(state.animeDetail is Resource.Success)
        assertEquals("Updated Title", state.animeDetail.data?.data?.title)

        coVerify(exactly = 1) { repository.getAnimeDetail(animeId) }
        coVerify(exactly = 1) { repository.getUpdatedAnimeDetailById(animeId) }
        coVerify(exactly = 1) { repository.loadAllEpisodes(any(), any()) }
    }


    @Test
    fun `loadAnimeDetail error should update state and not trigger loadAllEpisodes`() = runTest {
        val animeId = 1
        coEvery { repository.getAnimeDetail(animeId) } returns Pair(Resource.Error("Network Error"), false)
        viewModel = AnimeDetailViewModel(repository, workerScheduler)

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue(state.animeDetail is Resource.Error)
        assertEquals("Network Error", state.animeDetail.message)
        assertTrue(state.animeDetailComplement is Resource.Loading)

        coVerify(exactly = 1) { repository.getAnimeDetail(animeId) }
        coVerify(exactly = 0) { repository.loadAllEpisodes(any(), any()) }
    }

    @Test
    fun `toggleFavorite should call repository and update state`() = runTest {
        val animeId = 1
        val updatedComplement = mockAnimeComplement.copy(isFavorite = true)
        coEvery { repository.getAnimeDetail(animeId) } returns Pair(Resource.Success(mockAnimeResponse), false)
        coEvery { repository.loadAllEpisodes(any(), any()) } returns LoadEpisodesResult.Success(mockAnimeComplement)
        coEvery { repository.toggleAnimeFavorite(any(), any(), true) } returns updatedComplement
        viewModel = AnimeDetailViewModel(repository, workerScheduler)
        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        viewModel.onAction(DetailAction.ToggleFavorite(true))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue((state.animeDetailComplement as? Resource.Success)?.data?.isFavorite == true)
        coVerify(exactly = 1) { repository.toggleAnimeFavorite(any(), animeId, true) }
    }

    @Test
    fun `updateEpisodeQueryState should call FilterUtils and update state`() = runTest {
        val query = EpisodeFilterState(episodeQuery = FilterUtils.EpisodeQueryState(title = "test"))
        val episodes = listOf(episodePlaceholder)
        val filteredEpisodes = listOf(episodePlaceholder)
        val initialComplement = mockAnimeComplement.copy(episodes = episodes)

        coEvery { repository.getAnimeDetail(any()) } returns Pair(Resource.Success(mockAnimeResponse), false)
        coEvery { repository.loadAllEpisodes(any(), any()) } returns LoadEpisodesResult.Success(initialComplement)
        every { FilterUtils.filterEpisodes(episodes.reversed(), query.episodeQuery, any()) } returns filteredEpisodes

        viewModel = AnimeDetailViewModel(repository, workerScheduler)
        viewModel.onAction(DetailAction.LoadAnimeDetail(1))
        advanceUntilIdle()

        viewModel.onAction(DetailAction.UpdateEpisodeQueryState(query.episodeQuery))
        advanceUntilIdle()

        val state = viewModel.episodeFilterState.value
        assertEquals(query.episodeQuery, state.episodeQuery)
        assertEquals(filteredEpisodes, state.filteredEpisodes)
        verify { FilterUtils.filterEpisodes(episodes.reversed(), query.episodeQuery, any()) }
    }
}