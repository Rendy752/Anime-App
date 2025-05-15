package com.example.animeapp.episodeHistory

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.animeapp.models.*
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.ui.episodeHistory.EpisodeHistoryAction
import com.example.animeapp.ui.episodeHistory.EpisodeHistoryViewModel
import com.example.animeapp.utils.AnimeTitleFinder
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
        animeTitle = "Naruto: Shippuuden",
        episodeTitle = "Episode 1",
        imageUrl = "test_image_url",
        number = 1,
        isFiller = false,
        servers = episodeServersResponsePlaceholder,
        sources = episodeSourcesResponsePlaceholder,
        sourcesQuery = episodeSourcesQueryPlaceholder,
        isFavorite = false,
        lastWatched = "2025-05-12T23:54:02.306274",
        lastTimestamp = 600,
        updatedAt = 1620950042
    )

    private val mockEpisodeDetailComplement2 = mockEpisodeDetailComplement.copy(
        id = "lorem-ipsum-124?ep=124",
        episodeTitle = "Episode 2",
        number = 2,
        isFavorite = true,
        lastWatched = "2025-05-11T23:54:02.306274",
        lastTimestamp = 700
    )

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        mockkObject(ComplementUtils)
        mockkObject(AnimeTitleFinder)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(ComplementUtils)
        unmockkObject(AnimeTitleFinder)
        clearAllMocks()
    }

    @Test
    fun `FetchHistory should update episodeHistoryResults with grouped episodes and pagination`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.getEpisodeHistoryCount(null) } returns Resource.Success(1)
        coEvery { ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1) } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Success)
        val results = (state.episodeHistoryResults as Resource.Success).data
        assertTrue(results.containsKey(mockAnimeDetailComplement))
        assertEquals(listOf(mockEpisodeDetailComplement), results[mockAnimeDetailComplement])
        assertEquals(1, state.pagination?.items?.total)
        assertEquals(1, state.pagination?.items?.count)
        assertEquals(1, state.pagination?.last_visible_page)
        assertFalse(state.pagination?.has_next_page != false)
        coVerify(exactly = 1) { repository.getAllEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(null) }
        coVerify(exactly = 0) { AnimeTitleFinder.searchTitle<EpisodeDetailComplement>(any(), any(), any()) }
    }

    @Test
    fun `FetchHistory with search query should filter episodes using AnimeTitleFinder`() = runTest {
        val queryState = EpisodeHistoryQueryState(
            searchQuery = "Naruto",
            isFavorite = null,
            sortBy = EpisodeHistoryQueryState.SortBy.NewestFirst,
            page = 1,
            limit = 10
        )
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2)
        )
        coEvery { repository.getEpisodeHistoryCount(null) } returns Resource.Success(2)
        coEvery { AnimeTitleFinder.searchTitle<EpisodeDetailComplement>("Naruto", any(), any()) } returns listOf(mockEpisodeDetailComplement)
        coEvery { ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1) } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.ApplyFilters(queryState))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue("Results should be Success, but got ${state.episodeHistoryResults}", state.episodeHistoryResults is Resource.Success)
        val results = (state.episodeHistoryResults as Resource.Success).data
        assertTrue("Results should contain mockAnimeDetailComplement", results.containsKey(mockAnimeDetailComplement))
        assertEquals("Results should contain mockEpisodeDetailComplement", listOf(mockEpisodeDetailComplement), results[mockAnimeDetailComplement])
        assertEquals("Pagination count should be 1", 1, state.pagination?.items?.count)
        assertEquals("Pagination total should be 1", 1, state.pagination?.items?.total)
        assertEquals("Last visible page should be 1", 1, state.pagination?.last_visible_page)
        assertFalse("Has next page should be false", state.pagination?.has_next_page != false)
        coVerify(exactly = 1) { repository.getAllEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(null) }
        coVerify(exactly = 1) { AnimeTitleFinder.searchTitle<EpisodeDetailComplement>("Naruto", any(), any()) }
        assertEquals(queryState, state.queryState)
    }

    @Test
    fun `FetchHistory with isFavorite filter should return only favorite episodes`() = runTest {
        val queryState = EpisodeHistoryQueryState(
            searchQuery = "",
            isFavorite = true,
            sortBy = EpisodeHistoryQueryState.SortBy.NewestFirst,
            page = 1,
            limit = 10
        )
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2)
        )
        coEvery { repository.getEpisodeHistoryCount(true) } returns Resource.Success(1)
        coEvery { ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1) } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.ApplyFilters(queryState))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Success)
        val results = (state.episodeHistoryResults as Resource.Success).data
        assertTrue(results.containsKey(mockAnimeDetailComplement))
        assertEquals(listOf(mockEpisodeDetailComplement2), results[mockAnimeDetailComplement])
        assertEquals(1, state.pagination?.items?.total)
        assertEquals(1, state.pagination?.items?.count)
        assertEquals(1, state.pagination?.last_visible_page)
        assertFalse(state.pagination?.has_next_page != false)
        coVerify(exactly = 1) { repository.getAllEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(true) }
        coVerify(exactly = 0) { AnimeTitleFinder.searchTitle<EpisodeDetailComplement>(any(), any(), any()) }
        assertEquals(queryState, state.queryState)
    }

    @Test
    fun `FetchHistory with error should update episodeHistoryResults with error`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Error("Database error")
        coEvery { repository.getEpisodeHistoryCount(null) } returns Resource.Success(1)

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Error)
        assertEquals("Database error", (state.episodeHistoryResults as Resource.Error).message)
        assertEquals(null, state.pagination)
        coVerify(exactly = 1) { repository.getAllEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(null) }
    }

    @Test
    fun `ApplyFilters should update queryState and trigger FetchHistory`() = runTest {
        val updatedQueryState = EpisodeHistoryQueryState(
            searchQuery = "Naruto",
            isFavorite = true,
            sortBy = EpisodeHistoryQueryState.SortBy.EpisodeTitle,
            page = 1,
            limit = 10
        )
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2)
        )
        coEvery { repository.getEpisodeHistoryCount(true) } returns Resource.Success(1)
        coEvery { AnimeTitleFinder.searchTitle<EpisodeDetailComplement>("Naruto", any(), any()) } returns listOf(mockEpisodeDetailComplement2)
        coEvery { ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1) } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.ApplyFilters(updatedQueryState))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertEquals(updatedQueryState, state.queryState)
        assertTrue(state.episodeHistoryResults is Resource.Success)
        val results = (state.episodeHistoryResults as Resource.Success).data
        assertTrue(results.containsKey(mockAnimeDetailComplement))
        assertEquals(listOf(mockEpisodeDetailComplement2), results[mockAnimeDetailComplement])
        assertEquals(1, state.pagination?.items?.total)
        assertEquals(1, state.pagination?.items?.count)
        assertEquals(1, state.pagination?.current_page)
        coVerify(exactly = 1) { repository.getAllEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(true) }
        coVerify(exactly = 1) { AnimeTitleFinder.searchTitle<EpisodeDetailComplement>("Naruto", any(), any()) }
    }

    @Test
    fun `ChangePage should update page and fetch correct episodes`() = runTest {
        val episodes = List(15) { i ->
            mockEpisodeDetailComplement.copy(
                id = "ep_$i",
                episodeTitle = "Episode ${i + 1}",
                number = i + 1,
                lastWatched = "2025-05-12T23:54:02.306274",
                lastTimestamp = 600L - i
            )
        }
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(episodes)
        coEvery { repository.getEpisodeHistoryCount(null) } returns Resource.Success(15)
        coEvery { ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1) } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.ChangePage(2))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertEquals(2, state.queryState.page)
        assertTrue(state.episodeHistoryResults is Resource.Success)
        val results = (state.episodeHistoryResults as Resource.Success).data
        assertTrue(results.containsKey(mockAnimeDetailComplement))
        val pageEpisodes = results[mockAnimeDetailComplement] ?: emptyList()
        assertEquals(5, pageEpisodes.size)
        assertEquals("Episode 11", pageEpisodes.first().episodeTitle)
        assertEquals(15, state.pagination?.items?.total)
        assertEquals(5, state.pagination?.items?.count)
        assertEquals(2, state.pagination?.last_visible_page)
        assertFalse(state.pagination?.has_next_page != false)
        coVerify(exactly = 1) { repository.getAllEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(null) }
    }

    @Test
    fun `ToggleEpisodeFavorite should update episode favorite status and refresh history`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.getEpisodeHistoryCount(null) } returns Resource.Success(1)
        coEvery { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns mockEpisodeDetailComplement
        coEvery { repository.updateEpisodeDetailComplement(any()) } just Runs
        coEvery { ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1) } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.ToggleEpisodeFavorite("lorem-ipsum-123?ep=123", true))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Success)
        val results = (state.episodeHistoryResults as Resource.Success).data
        val updatedEpisode = results[mockAnimeDetailComplement]?.first()
        assertEquals(true, updatedEpisode?.isFavorite)
        coVerify(exactly = 1) { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
        coVerify(exactly = 1) { repository.updateEpisodeDetailComplement(any()) }
        coVerify(exactly = 1) { repository.getAllEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(null) }
    }

    @Test
    fun `ToggleEpisodeFavorite with error should update episodeHistoryResults with error`() = runTest {
        coEvery { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns null

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.ToggleEpisodeFavorite("lorem-ipsum-123?ep=123", true))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Error)
        assertEquals("Episode not found", (state.episodeHistoryResults as Resource.Error).message)
        coVerify(exactly = 1) { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
        coVerify(exactly = 0) { repository.updateEpisodeDetailComplement(any()) }
    }

    @Test
    fun `ToggleAnimeFavorite should update anime favorite status and refresh history`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.getEpisodeHistoryCount(null) } returns Resource.Success(1)
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
        coEvery { repository.updateCachedAnimeDetailComplement(any()) } just Runs
        coEvery { ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1) } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.ToggleAnimeFavorite(1, true))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Success)
        val results = (state.episodeHistoryResults as Resource.Success).data
        val updatedAnime = results.keys.first { it.malId == 1 }
        assertEquals(true, updatedAnime.isFavorite)
        coVerify(exactly = 1) { repository.getCachedAnimeDetailComplementByMalId(1) }
        coVerify(exactly = 1) { repository.updateCachedAnimeDetailComplement(any()) }
        coVerify(exactly = 1) { repository.getAllEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(null) }
    }

    @Test
    fun `ToggleAnimeFavorite with error should update episodeHistoryResults with error`() = runTest {
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns null

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.ToggleAnimeFavorite(1, true))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Error)
        assertEquals("Anime not found", (state.episodeHistoryResults as Resource.Error).message)
        coVerify(exactly = 1) { repository.getCachedAnimeDetailComplementByMalId(1) }
        coVerify(exactly = 0) { repository.updateCachedAnimeDetailComplement(any()) }
    }

    @Test
    fun `DeleteEpisode should remove episode from state when deletion is successful`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2)
        )
        coEvery { repository.getEpisodeHistoryCount(null) } returns Resource.Success(2)
        coEvery { repository.deleteEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns true
        coEvery { ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1) } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.DeleteEpisode("lorem-ipsum-123?ep=123"))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Success)
        val results = (state.episodeHistoryResults as Resource.Success).data
        assertTrue(results[mockAnimeDetailComplement]?.any { it.id == "lorem-ipsum-124?ep=124" } == true)
        assertFalse(results[mockAnimeDetailComplement]?.any { it.id == "lorem-ipsum-123?ep=123" } == true)
        coVerify(exactly = 1) { repository.deleteEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
        coVerify(exactly = 1) { repository.getAllEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(null) }
    }

    @Test
    fun `DeleteEpisode with error should update episodeHistoryResults with error`() = runTest {
        coEvery { repository.deleteEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns false

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.DeleteEpisode("lorem-ipsum-123?ep=123"))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Error)
        assertEquals("Episode not found", (state.episodeHistoryResults as Resource.Error).message)
        coVerify(exactly = 1) { repository.deleteEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
    }

    @Test
    fun `DeleteAnime should remove anime from state when deletion is successful`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.getEpisodeHistoryCount(null) } returns Resource.Success(1)
        coEvery { repository.deleteAnimeDetailComplement(1) } returns true
        coEvery { ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1) } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.DeleteAnime(1))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Success)
        val results = (state.episodeHistoryResults as Resource.Success).data
        assertFalse(results.any { it.key.malId == 1 })
        coVerify(exactly = 1) { repository.deleteAnimeDetailComplement(1) }
        coVerify(exactly = 1) { repository.getAllEpisodeHistory(any()) }
        coVerify(exactly = 1) { repository.getEpisodeHistoryCount(null) }
    }

    @Test
    fun `DeleteAnime with error should update episodeHistoryResults with error`() = runTest {
        coEvery { repository.deleteAnimeDetailComplement(1) } returns false

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.DeleteAnime(1))
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertTrue(state.episodeHistoryResults is Resource.Error)
        assertEquals("Anime not found", (state.episodeHistoryResults as Resource.Error).message)
        coVerify(exactly = 1) { repository.deleteAnimeDetailComplement(1) }
    }
}