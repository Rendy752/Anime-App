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
import org.junit.Assert.assertFalse

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
                    sortBy = EpisodeHistoryQueryState.SortBy.NewestFirst,
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
        coEvery { repository.getPaginatedEpisodeHistory(capture(queryStateSlot)) } returns Resource.Error(
            "Database error"
        )
        coEvery { repository.getEpisodeHistoryCount(capture(searchQuerySlot), any()) } answers {
            capturedIsFavorite = arg(1)
            Resource.Success(1)
        }
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
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
                sortBy = EpisodeHistoryQueryState.SortBy.NewestFirst,
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
            sortBy = EpisodeHistoryQueryState.SortBy.EpisodeTitle,
            page = 2,
            limit = 10
        )
        val queryStateSlot = slot<EpisodeHistoryQueryState>()
        coEvery { repository.getPaginatedEpisodeHistory(capture(queryStateSlot)) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(1)
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
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
            coEvery { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns mockEpisodeDetailComplement
            coEvery { repository.updateEpisodeDetailComplement(any()) } just Runs
            coEvery {
                ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
            } returns mockAnimeDetailComplement

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
            val results = (state.episodeHistoryResults as Resource.Success).data
            val updatedEpisode = results[mockAnimeDetailComplement]?.first()
            assertEquals(true, updatedEpisode?.isFavorite)
            coVerify(exactly = 1) { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
            coVerify(exactly = 1) { repository.updateEpisodeDetailComplement(any()) }
            coVerify(exactly = 1) { repository.getPaginatedEpisodeHistory(any()) }
            coVerify(exactly = 1) { repository.getEpisodeHistoryCount(any(), any()) }
        }

    @Test
    fun `ToggleEpisodeFavorite with error should update episodeHistoryResults with error`() =
        runTest {
            coEvery { repository.getPaginatedEpisodeHistory(any()) } returns Resource.Success(
                listOf(mockEpisodeDetailComplement)
            )
            coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(1)
            coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
            coEvery { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns null
            coEvery {
                ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
            } returns mockAnimeDetailComplement

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
            coVerify(exactly = 1) { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
            coVerify(exactly = 0) { repository.updateEpisodeDetailComplement(any()) }
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
        coEvery { repository.updateCachedAnimeDetailComplement(any()) } just Runs
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.ToggleAnimeFavorite(1, true))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Success)
        val results = (state.episodeHistoryResults as Resource.Success).data
        val updatedAnime = results.keys.first { it.malId == 1 }
        assertEquals(true, updatedAnime.isFavorite)
        coVerify(exactly = 1) { repository.getCachedAnimeDetailComplementByMalId(1) }
        coVerify(exactly = 1) { repository.updateCachedAnimeDetailComplement(any()) }
        coVerify(exactly = 1) { repository.getPaginatedEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(any(), any()) }
    }

    @Test
    fun `ToggleAnimeFavorite with error should update episodeHistoryResults with error`() =
        runTest {
            coEvery { repository.getPaginatedEpisodeHistory(any()) } returns Resource.Success(
                listOf(mockEpisodeDetailComplement)
            )
            coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(1)
            coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns null
            coEvery {
                ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
            } returns mockAnimeDetailComplement

            viewModel = EpisodeHistoryViewModel(repository)
            viewModel.onAction(EpisodeHistoryAction.ToggleAnimeFavorite(1, true))
            advanceUntilIdle()

            val state = viewModel.historyState.value
            assertTrue(state.episodeHistoryResults is Resource.Error)
            assertEquals(
                "Anime not found",
                (state.episodeHistoryResults as Resource.Error).message
            )
            coVerify(exactly = 1) { repository.getCachedAnimeDetailComplementByMalId(1) }
            coVerify(exactly = 0) { repository.updateCachedAnimeDetailComplement(any()) }
            coVerify(exactly = 1) { repository.getPaginatedEpisodeHistory(any()) }
            coVerify(exactly = 1) { repository.getEpisodeHistoryCount(any(), any()) }
        }

    @Test
    fun `DeleteEpisode should remove episode from state when deletion is successful`() = runTest {
        coEvery { repository.getPaginatedEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(1)
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
        coEvery { repository.deleteEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns true
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.DeleteEpisode("lorem-ipsum-123?ep=123"))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Success)
        val results = (state.episodeHistoryResults as Resource.Success).data
        assertFalse(results.any { it.value.any { episode -> episode.id == "lorem-ipsum-123?ep=123" } })
        coVerify(exactly = 1) { repository.deleteEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
        coVerify(exactly = 1) { repository.getPaginatedEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(any(), any()) }
    }

    @Test
    fun `DeleteEpisode with error should update episodeHistoryResults with error`() = runTest {
        coEvery { repository.getPaginatedEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(1)
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
        coEvery { repository.deleteEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns false
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.DeleteEpisode("lorem-ipsum-123?ep=123"))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Error)
        assertEquals(
            "Episode not found",
            (state.episodeHistoryResults as Resource.Error).message
        )
        coVerify(exactly = 1) { repository.deleteEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
        coVerify(exactly = 1) { repository.getPaginatedEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(any(), any()) }
    }

    @Test
    fun `DeleteAnime should remove anime from state when deletion is successful`() = runTest {
        coEvery { repository.getPaginatedEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(1)
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
        coEvery { repository.deleteAnimeDetailComplement(1) } returns true
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.DeleteAnime(1))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Success)
        val results = (state.episodeHistoryResults as Resource.Success).data
        assertFalse(results.any { it.key.malId == 1 })
        coVerify(exactly = 1) { repository.deleteAnimeDetailComplement(1) }
        coVerify(exactly = 1) { repository.getPaginatedEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(any(), any()) }
    }

    @Test
    fun `DeleteAnime with error should update episodeHistoryResults with error`() = runTest {
        coEvery { repository.getPaginatedEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(1)
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
        coEvery { repository.deleteAnimeDetailComplement(1) } returns false
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.DeleteAnime(1))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Error)
        assertEquals(
            "Anime not found",
            (state.episodeHistoryResults as Resource.Error).message
        )
        coVerify(exactly = 1) { repository.deleteAnimeDetailComplement(1) }
        coVerify(exactly = 1) { repository.getPaginatedEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(any(), any()) }
    }

    @Test
    fun `DeleteEpisode with multiple episodes removes only specified episode`() = runTest {
        val episode2 = mockEpisodeDetailComplement.copy(id = "lorem-ipsum-124?ep=124")
        coEvery { repository.getPaginatedEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement, episode2)
        )
        coEvery { repository.getEpisodeHistoryCount(any(), any()) } returns Resource.Success(2)
        coEvery { repository.deleteEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns true
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = any(), malId = any())
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.DeleteEpisode("lorem-ipsum-123?ep=123"))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        val results = (state.episodeHistoryResults as Resource.Success).data
        assertTrue(results[mockAnimeDetailComplement]?.any { it.id == "lorem-ipsum-124?ep=124" } == true)
        assertFalse(results[mockAnimeDetailComplement]?.any { it.id == "lorem-ipsum-123?ep=123" } == true)
        coVerify(exactly = 1) { repository.getPaginatedEpisodeHistory(any()) }
    }
}