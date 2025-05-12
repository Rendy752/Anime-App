package com.example.animeapp.episodeHistory

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.animeapp.models.*
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.ui.episodeHistory.EpisodeHistoryAction
import com.example.animeapp.ui.episodeHistory.EpisodeHistoryViewModel
import com.example.animeapp.utils.ComplementUtils
import com.example.animeapp.utils.Resource
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
class EpisodeHistoryViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: EpisodeHistoryViewModel
    private lateinit var repository: AnimeEpisodeDetailRepository
    private lateinit var testDispatcher: TestDispatcher

    private val mockAnimeDetailComplement = AnimeDetailComplement(
        id = "aniwatch1",
        malId = 1,
        lastEpisodeWatchedId = "lorem-ipsum-123?ep=123",
        episodes = listOf(
            Episode(
                episodeId = "lorem-ipsum-123?ep=123",
                episodeNo = 1,
                name = "Episode 1",
                filler = false
            )
        ),
        isFavorite = false
    )
    private val mockEpisodeDetailComplement = EpisodeDetailComplement(
        id = "lorem-ipsum-123?ep=123",
        malId = 1,
        aniwatchId = "aniwatch1",
        animeTitle = "Test Anime",
        episodeTitle = "Episode 1",
        imageUrl = "test_image_url",
        number = 1,
        isFiller = false,
        servers = episodeServersResponsePlaceholder,
        sources = episodeSourcesResponsePlaceholder,
        sourcesQuery = episodeSourcesQueryPlaceholder,
        isFavorite = false,
        lastWatched = "2025-05-12T23:54:02.306274",
        lastTimestamp = 600
    )

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        mockkObject(ComplementUtils)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(ComplementUtils)
        clearAllMocks()
    }

    @Test
    fun `FetchHistory should update episodeHistoryResults with grouped episodes and pagination`() =
        runTest {
            val queryStateSlot = slot<EpisodeHistoryQueryState>()
            coEvery { repository.getPaginatedEpisodeHistory(capture(queryStateSlot)) } returns Resource.Success(
                listOf(mockEpisodeDetailComplement)
            )
            coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(1)
            coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
            coEvery { repository.updateEpisodeDetailComplement(any()) } just Runs
            coEvery { repository.updateCachedAnimeDetailComplement(any()) } just Runs
            coEvery {
                ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
            } returns mockAnimeDetailComplement

            viewModel = EpisodeHistoryViewModel(repository)
            viewModel.onAction(EpisodeHistoryAction.FetchHistory)
            advanceUntilIdle()

            val state = viewModel.historyState.value
            assertTrue(state.episodeHistoryResults is Resource.Success)
            val results = (state.episodeHistoryResults as Resource.Success).data
            assertTrue(results.containsKey(mockAnimeDetailComplement))
            assertEquals(listOf(mockEpisodeDetailComplement), results[mockAnimeDetailComplement])
            assertEquals(1, state.pagination?.items?.total)
            assertEquals(1, state.pagination?.last_visible_page)
            assertEquals(false, state.pagination?.has_next_page)
            coVerify(exactly = 2) { repository.getPaginatedEpisodeHistory(any()) }
            coVerify(exactly = 2) { repository.getEpisodeHistoryCount(any(), any()) }
            assertEquals(
                EpisodeHistoryQueryState(
                    searchQuery = "",
                    isFavorite = null,
                    sortBy = EpisodeHistoryQueryState.SortBy.LastWatchedDesc,
                    page = 1,
                    limit = 10
                ),
                queryStateSlot.captured
            )
        }

    @Test
    fun `FetchHistory with error should update episodeHistoryResults with error`() = runTest {
        val queryStateSlot = slot<EpisodeHistoryQueryState>()
        val searchQuerySlot = slot<String>()
        var capturedIsFavorite: Boolean? = null
        coEvery { repository.getPaginatedEpisodeHistory(capture(queryStateSlot)) } returns Resource.Error("Database error")
        coEvery { repository.getEpisodeHistoryCount(capture(searchQuerySlot), any()) } answers {
            capturedIsFavorite = arg(1)
            Resource.Success(1)
        }
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
        coEvery { repository.updateEpisodeDetailComplement(any()) } just Runs
        coEvery { repository.updateCachedAnimeDetailComplement(any()) } just Runs
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Error)
        assertEquals("Database error", (state.episodeHistoryResults as Resource.Error).message)
        assertEquals(null, state.pagination)
        coVerify(exactly = 2) { repository.getPaginatedEpisodeHistory(any()) }
        coVerify(exactly = 2) { repository.getEpisodeHistoryCount(any(), any()) }
        assertEquals(
            EpisodeHistoryQueryState(
                searchQuery = "",
                isFavorite = null,
                sortBy = EpisodeHistoryQueryState.SortBy.LastWatchedDesc,
                page = 1,
                limit = 10
            ),
            queryStateSlot.captured
        )
        assertEquals("", searchQuerySlot.captured)
        assertEquals(null, capturedIsFavorite)
    }

    @Test
    fun `ApplyFilters should update queryState and trigger FetchHistory`() = runTest {
        val updatedQueryState = EpisodeHistoryQueryState(
            searchQuery = "Test",
            isFavorite = true,
            sortBy = EpisodeHistoryQueryState.SortBy.EpisodeTitleAsc,
            page = 2,
            limit = 10
        )
        val queryStateSlot = slot<EpisodeHistoryQueryState>()
        coEvery { repository.getPaginatedEpisodeHistory(capture(queryStateSlot)) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(1)
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
        coEvery { repository.updateEpisodeDetailComplement(any()) } just Runs
        coEvery { repository.updateCachedAnimeDetailComplement(any()) } just Runs
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.ApplyFilters(updatedQueryState))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertEquals(updatedQueryState, state.queryState)
        assertTrue(state.episodeHistoryResults is Resource.Success)
        coVerify(exactly = 2) { repository.getPaginatedEpisodeHistory(any()) }
        coVerify(exactly = 2) { repository.getEpisodeHistoryCount(any(), any()) }
        assertEquals(updatedQueryState, queryStateSlot.captured)
    }

    @Test
    fun `ToggleEpisodeFavorite should update episode favorite status and refresh history`() =
        runTest {
            coEvery { repository.getPaginatedEpisodeHistory(any()) } returns Resource.Success(
                listOf(mockEpisodeDetailComplement)
            )
            coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(1)
            coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
            coEvery { repository.updateEpisodeDetailComplement(any()) } just Runs
            coEvery { repository.updateCachedAnimeDetailComplement(any()) } just Runs
            coEvery {
                ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
            } returns mockAnimeDetailComplement
            coEvery {
                ComplementUtils.toggleEpisodeFavorite(
                    repository,
                    "lorem-ipsum-123?ep=123",
                    true
                )
            } returns mockEpisodeDetailComplement.copy(isFavorite = true)

            viewModel = EpisodeHistoryViewModel(repository)
            viewModel.onAction(
                EpisodeHistoryAction.ToggleEpisodeFavorite(
                    "lorem-ipsum-123?ep=123",
                    true
                )
            )
            advanceUntilIdle()

            val state = viewModel.historyState.value
            assertTrue(state.episodeHistoryResults is Resource.Success)
            coVerify(exactly = 1) {
                ComplementUtils.toggleEpisodeFavorite(
                    repository,
                    "lorem-ipsum-123?ep=123",
                    true
                )
            }
            coVerify(exactly = 2) { repository.getPaginatedEpisodeHistory(any()) }
            coVerify(exactly = 2) { repository.getEpisodeHistoryCount(any(), any()) }
        }

    @Test
    fun `ToggleEpisodeFavorite with error should update episodeHistoryResults with error`() =
        runTest {
            coEvery { repository.getPaginatedEpisodeHistory(any()) } returns Resource.Success(
                listOf(mockEpisodeDetailComplement)
            )
            coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(1)
            coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
            coEvery { repository.updateEpisodeDetailComplement(any()) } just Runs
            coEvery { repository.updateCachedAnimeDetailComplement(any()) } just Runs
            coEvery {
                ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
            } returns mockAnimeDetailComplement
            coEvery {
                ComplementUtils.toggleEpisodeFavorite(
                    repository,
                    "lorem-ipsum-123?ep=123",
                    true
                )
            } returns null

            viewModel = EpisodeHistoryViewModel(repository)
            viewModel.onAction(
                EpisodeHistoryAction.ToggleEpisodeFavorite(
                    "lorem-ipsum-123?ep=123",
                    true
                )
            )
            advanceUntilIdle()

            val state = viewModel.historyState.value
            assertTrue(state.episodeHistoryResults is Resource.Error)
            assertEquals(
                "Episode not found",
                (state.episodeHistoryResults as Resource.Error).message
            )
            coVerify(exactly = 1) {
                ComplementUtils.toggleEpisodeFavorite(
                    repository,
                    "lorem-ipsum-123?ep=123",
                    true
                )
            }
            coVerify(exactly = 1) { repository.getPaginatedEpisodeHistory(any()) }
            coVerify(exactly = 1) { repository.getEpisodeHistoryCount(any(), any()) }
        }

    @Test
    fun `ToggleAnimeFavorite should update anime favorite status and refresh history`() = runTest {
        coEvery { repository.getPaginatedEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(1)
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
        coEvery { repository.updateEpisodeDetailComplement(any()) } just Runs
        coEvery { repository.updateCachedAnimeDetailComplement(any()) } just Runs
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement
        coEvery {
            ComplementUtils.toggleAnimeFavorite(
                repository = repository, malId = 1, isFavorite = true
            )
        } returns mockAnimeDetailComplement.copy(isFavorite = true)

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.ToggleAnimeFavorite(1, true))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Success)
        coVerify(exactly = 1) {
            ComplementUtils.toggleAnimeFavorite(
                repository = repository, malId = 1, isFavorite = true
            )
        }
        coVerify(exactly = 2) { repository.getPaginatedEpisodeHistory(any()) }
        coVerify(exactly = 2) { repository.getEpisodeHistoryCount(any(), any()) }
    }

    @Test
    fun `ToggleAnimeFavorite with error should update episodeHistoryResults with error`() =
        runTest {
            coEvery { repository.getPaginatedEpisodeHistory(any()) } returns Resource.Success(
                listOf(mockEpisodeDetailComplement)
            )
            coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(1)
            coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
            coEvery { repository.updateEpisodeDetailComplement(any()) } just Runs
            coEvery { repository.updateCachedAnimeDetailComplement(any()) } just Runs
            coEvery {
                ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
            } returns mockAnimeDetailComplement
            coEvery {
                ComplementUtils.toggleAnimeFavorite(
                    repository = repository, malId = 1, isFavorite = true
                )
            } returns null

            viewModel = EpisodeHistoryViewModel(repository)
            viewModel.onAction(EpisodeHistoryAction.ToggleAnimeFavorite(1, true))
            advanceUntilIdle()

            val state = viewModel.historyState.value
            assertTrue(state.episodeHistoryResults is Resource.Error)
            assertEquals(
                "Failed to update anime",
                (state.episodeHistoryResults as Resource.Error).message
            )
            coVerify(exactly = 1) {
                ComplementUtils.toggleAnimeFavorite(
                    repository = repository, malId = 1, isFavorite = true
                )
            }
            coVerify(exactly = 1) { repository.getPaginatedEpisodeHistory(any()) }
            coVerify(exactly = 1) { repository.getEpisodeHistoryCount(any(), any()) }
        }
}