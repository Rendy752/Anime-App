package com.luminoverse.animevibe.episodeHistory

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.repository.EpisodeHistoryResult
import com.luminoverse.animevibe.ui.episodeHistory.EpisodeHistoryAction
import com.luminoverse.animevibe.ui.episodeHistory.EpisodeHistoryViewModel
import com.luminoverse.animevibe.utils.resource.Resource
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class EpisodeHistoryViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: EpisodeHistoryViewModel
    private lateinit var repository: AnimeEpisodeDetailRepository
    private lateinit var testDispatcher: TestDispatcher

    private val mockAnimeComplement = AnimeDetailComplement("aniwatch1", 1)
    private val mockEpisodeComplement = EpisodeDetailComplement(
        id = "ep1",
        malId = 1,
        aniwatchId = "aniwatch1",
        animeTitle = "Anime 1",
        episodeTitle = "Episode 1",
        imageUrl = "url",
        number = 1,
        isFiller = false,
        servers = emptyList(),
        sources = episodeSourcesPlaceholder,
        sourcesQuery = episodeSourcesQueryPlaceholder
    )
    private val mockHistoryResult = EpisodeHistoryResult(
        data = mapOf(mockAnimeComplement to listOf(mockEpisodeComplement)),
        pagination = CompletePagination(1, false, 1, Items(1, 1, 10))
    )

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = EpisodeHistoryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `FetchHistory action successfully updates state with paginated data`() = runTest {
        coEvery { repository.getPaginatedAndFilteredHistory(any()) } returns Resource.Success(
            mockHistoryResult
        )
        viewModel = EpisodeHistoryViewModel(repository)

        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(paginatedHistory is Resource.Success)
            val successData = (paginatedHistory as Resource.Success).data
            assertEquals(mockHistoryResult.data, successData)
            assertEquals(mockHistoryResult.pagination, pagination)
            assertFalse(isRefreshing)
        }
        coVerify(exactly = 1) { repository.getPaginatedAndFilteredHistory(any()) }
    }

    @Test
    fun `FetchHistory action with error updates state correctly`() = runTest {
        coEvery { repository.getPaginatedAndFilteredHistory(any()) } returns Resource.Error("Database Error")
        viewModel = EpisodeHistoryViewModel(repository)

        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(paginatedHistory is Resource.Error)
            assertEquals("Database Error", (paginatedHistory as Resource.Error).message)
            assertFalse(isRefreshing)
        }
    }

    @Test
    fun `ApplyFilters action updates query state and refetches history when history is not empty`() = runTest {
        val newQuery = EpisodeHistoryQueryState(searchQuery = "Test")
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(listOf(mockEpisodeComplement))
        coEvery { repository.getPaginatedAndFilteredHistory(any()) } returns Resource.Success(mockHistoryResult)

        viewModel.onAction(EpisodeHistoryAction.CheckIfHistoryIsEmpty)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.ApplyFilters(newQuery))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertEquals(newQuery.copy(page = 1), queryState)
        }
        coVerify(exactly = 1) {
            repository.getPaginatedAndFilteredHistory(withArg {
                assertEquals("Test", it.searchQuery)
            })
        }
    }

    @Test
    fun `ChangePage action updates query state and refetches history`() = runTest {
        val page2Result = mockHistoryResult.copy(
            pagination = mockHistoryResult.pagination.copy(current_page = 2)
        )
        coEvery { repository.getPaginatedAndFilteredHistory(match { it.page == 2 }) } returns Resource.Success(page2Result)

        viewModel.onAction(EpisodeHistoryAction.ChangePage(2))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertEquals(2, queryState.page)
            assertEquals(2, pagination.current_page)
        }
        coVerify(exactly = 1) {
            repository.getPaginatedAndFilteredHistory(withArg {
                assertEquals(2, it.page)
            })
        }
    }

    @Test
    fun `ToggleEpisodeFavorite action calls repository and refetches history`() = runTest {
        val episodeId = "ep1"
        coEvery { repository.toggleEpisodeFavorite(episodeId, true) } returns Resource.Success(Unit)
        coEvery { repository.getPaginatedAndFilteredHistory(any()) } returns Resource.Success(
            mockHistoryResult
        )
        viewModel = EpisodeHistoryViewModel(repository)

        viewModel.onAction(EpisodeHistoryAction.ToggleEpisodeFavorite(episodeId, true))
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.toggleEpisodeFavorite(episodeId, true) }
        coVerify(exactly = 1) { repository.getPaginatedAndFilteredHistory(any()) }
    }

    @Test
    fun `ToggleAnimeFavorite action calls repository and refetches history`() = runTest {
        val malId = 1
        coEvery { repository.getCachedAnimeDetailComplementByMalId(malId) } returns mockAnimeComplement
        coEvery { repository.toggleAnimeFavorite(any(), malId, true) } returns mockAnimeComplement
        coEvery { repository.getPaginatedAndFilteredHistory(any()) } returns Resource.Success(
            mockHistoryResult
        )
        viewModel = EpisodeHistoryViewModel(repository)

        viewModel.onAction(EpisodeHistoryAction.ToggleAnimeFavorite(malId, true))
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.toggleAnimeFavorite(
                mockAnimeComplement.id,
                malId,
                true
            )
        }
        coVerify(exactly = 1) { repository.getPaginatedAndFilteredHistory(any()) }
    }

    @Test
    fun `DeleteEpisode action calls repository and refetches history`() = runTest {
        val episodeId = "ep1"
        coEvery { repository.deleteEpisodeDetailComplement(episodeId) } returns true
        coEvery { repository.getPaginatedAndFilteredHistory(any()) } returns Resource.Success(
            mockHistoryResult
        )
        viewModel = EpisodeHistoryViewModel(repository)

        viewModel.onAction(EpisodeHistoryAction.DeleteEpisode(episodeId))
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteEpisodeDetailComplement(episodeId) }
        coVerify(exactly = 1) { repository.getPaginatedAndFilteredHistory(any()) }
    }

    @Test
    fun `DeleteAnime action calls repository and refetches history`() = runTest {
        val malId = 1
        coEvery { repository.deleteAnimeDetailById(malId) } just Runs
        coEvery { repository.deleteAnimeDetailComplement(malId) } returns true
        coEvery { repository.getPaginatedAndFilteredHistory(any()) } returns Resource.Success(
            mockHistoryResult
        )
        viewModel = EpisodeHistoryViewModel(repository)

        viewModel.onAction(EpisodeHistoryAction.DeleteAnime(malId))
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteAnimeDetailById(malId) }
        coVerify(exactly = 1) { repository.deleteAnimeDetailComplement(malId) }
        coVerify(exactly = 1) { repository.getPaginatedAndFilteredHistory(any()) }
    }

    @Test
    fun `CheckIfHistoryIsEmpty updates state correctly`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(
                mockEpisodeComplement
            )
        )
        viewModel = EpisodeHistoryViewModel(repository)

        viewModel.onAction(EpisodeHistoryAction.CheckIfHistoryIsEmpty)
        advanceUntilIdle()

        assertFalse(viewModel.historyState.value.isEpisodeHistoryEmpty)

        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(emptyList())

        viewModel.onAction(EpisodeHistoryAction.CheckIfHistoryIsEmpty)
        advanceUntilIdle()

        assertTrue(viewModel.historyState.value.isEpisodeHistoryEmpty)
    }
}