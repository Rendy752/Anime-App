package com.luminoverse.animevibe.episodeHistory

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.ui.episodeHistory.EpisodeHistoryAction
import com.luminoverse.animevibe.ui.episodeHistory.EpisodeHistoryViewModel
import com.luminoverse.animevibe.utils.watch.AnimeTitleFinder
import com.luminoverse.animevibe.utils.ComplementUtils
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

    private val mockAnimeDetailComplement = AnimeDetailComplement(
        id = "aniwatch1",
        malId = 1,
        lastEpisodeWatchedId = "lorem-ipsum-123?ep=123",
        episodes = listOf(
            Episode(
                id = "lorem-ipsum-123?ep=123",
                episode_no = 1,
                title = "Episode 1",
                japanese_title = "Episode 1",
                filler = false
            )
        ),
        isFavorite = false
    )

    private val mockAnimeDetailComplement2 = mockAnimeDetailComplement.copy(
        id = "aniwatch2",
        malId = 2,
        lastEpisodeWatchedId = "lorem-ipsum-124?ep=124"
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
        servers = listOf(episodeServerPlaceholder),
        sources = episodeSourcesPlaceholder,
        sourcesQuery = episodeSourcesQueryPlaceholder,
        isFavorite = false,
        lastWatched = "2025-05-12T23:54:02.306274",
        lastTimestamp = 600,
        updatedAt = 1620950042
    )

    private val mockEpisodeDetailComplement2 = mockEpisodeDetailComplement.copy(
        id = "lorem-ipsum-124?ep=124",
        malId = 2,
        aniwatchId = "aniwatch2",
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
    fun `FetchHistory updates state with grouped episodes and pagination`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Success)
            assertTrue(episodeHistoryResults is Resource.Success)
            val filteredResults = (filteredEpisodeHistoryResults as Resource.Success).data
            val episodeResults = (episodeHistoryResults as Resource.Success).data
            assertTrue(filteredResults.containsKey(mockAnimeDetailComplement))
            assertEquals(listOf(mockEpisodeDetailComplement), filteredResults[mockAnimeDetailComplement])
            assertEquals(listOf(mockEpisodeDetailComplement), episodeResults)
            assertEquals(1, pagination.items.total)
            assertEquals(1, pagination.items.count)
            assertEquals(1, pagination.last_visible_page)
            assertFalse(pagination.has_next_page)
            assertFalse(isRefreshing)
        }
        coVerify(exactly = 1) { repository.getAllEpisodeHistory(any()) }
    }

    @Test
    fun `FetchHistory with search query filters episodes using AnimeTitleFinder`() = runTest {
        val queryState = EpisodeHistoryQueryState(searchQuery = "Naruto", page = 1, limit = 10)
        val allEpisodes = listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2)

        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(allEpisodes)

        coEvery {
            AnimeTitleFinder.searchTitle<EpisodeDetailComplement>("Naruto", any(), any())
        } returns listOf(mockEpisodeDetailComplement)
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)

        viewModel.onAction(EpisodeHistoryAction.FetchAllHistory)
        advanceUntilIdle()

        viewModel.onAction(EpisodeHistoryAction.ApplyFilters(queryState))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Success)
            val filteredData = (filteredEpisodeHistoryResults as Resource.Success).data
            assertTrue(episodeHistoryResults is Resource.Success)
            val allData = (episodeHistoryResults as Resource.Success).data

            assertEquals(allEpisodes, allData)
            assertEquals(1, filteredData.size)
            assertTrue(filteredData.containsKey(mockAnimeDetailComplement))
            assertEquals(listOf(mockEpisodeDetailComplement), filteredData[mockAnimeDetailComplement])
            assertEquals(1, pagination.items.total)
            assertEquals(1, pagination.items.count)
            assertEquals(queryState.copy(page = pagination.current_page), this.queryState)
        }
        coVerify(exactly = 2) { repository.getAllEpisodeHistory(any()) }
        coVerify(exactly = 1) { AnimeTitleFinder.searchTitle<EpisodeDetailComplement>("Naruto", any(), any()) }
    }

    @Test
    fun `FetchHistory with isFavorite filter returns only favorite episodes`() = runTest {
        val queryState = EpisodeHistoryQueryState(isFavorite = true, page = 1, limit = 10)
        val allEpisodes = listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2)

        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(allEpisodes)
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 2)
        } returns mockAnimeDetailComplement2

        viewModel = EpisodeHistoryViewModel(repository)

        viewModel.onAction(EpisodeHistoryAction.FetchAllHistory)
        advanceUntilIdle()

        viewModel.onAction(EpisodeHistoryAction.ApplyFilters(queryState))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Success)
            val filteredData = (filteredEpisodeHistoryResults as Resource.Success).data
            assertTrue(episodeHistoryResults is Resource.Success)
            val allData = (episodeHistoryResults as Resource.Success).data

            assertEquals(allEpisodes, allData)
            assertEquals(1, filteredData.size)
            assertTrue(filteredData.containsKey(mockAnimeDetailComplement2))
            assertEquals(listOf(mockEpisodeDetailComplement2), filteredData[mockAnimeDetailComplement2])
            assertEquals(1, pagination.items.total)
            assertEquals(1, pagination.items.count)
            assertEquals(queryState.copy(page = pagination.current_page), this.queryState)
        }
        coVerify(exactly = 2) { repository.getAllEpisodeHistory(any()) }
        coVerify(exactly = 0) { AnimeTitleFinder.searchTitle<EpisodeDetailComplement>(any(), any(), any()) }
    }

    @Test
    fun `FetchHistory with error updates state with error`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Error("Database error")

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Error)
            assertTrue(episodeHistoryResults is Resource.Error)
            assertEquals("Database error", (filteredEpisodeHistoryResults as Resource.Error).message)
            assertEquals("Database error", (episodeHistoryResults as Resource.Error).message)
            assertEquals(defaultCompletePagination, pagination)
            assertFalse(isRefreshing)
        }
        coVerify(exactly = 1) { repository.getAllEpisodeHistory(any()) }
    }

    @Test
    fun `ApplyFilters updates queryState and triggers FetchHistory`() = runTest {
        val queryState = EpisodeHistoryQueryState(searchQuery = "Boruto", isFavorite = true, page = 1, limit = 10)
        val allEpisodes = listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2)

        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(allEpisodes)
        coEvery {
            AnimeTitleFinder.searchTitle<EpisodeDetailComplement>("Boruto", listOf(mockEpisodeDetailComplement2), any())
        } returns listOf(mockEpisodeDetailComplement2)
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 2)
        } returns mockAnimeDetailComplement2

        viewModel = EpisodeHistoryViewModel(repository)

        viewModel.onAction(EpisodeHistoryAction.FetchAllHistory)
        advanceUntilIdle()

        viewModel.onAction(EpisodeHistoryAction.ApplyFilters(queryState))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertEquals(queryState.copy(page = pagination.current_page), this.queryState)
            assertTrue(filteredEpisodeHistoryResults is Resource.Success)
            val filteredData = (filteredEpisodeHistoryResults as Resource.Success).data
            assertTrue(episodeHistoryResults is Resource.Success)
            val allData = (episodeHistoryResults as Resource.Success).data

            assertEquals(allEpisodes, allData)
            assertEquals(1, filteredData.size)
            assertTrue(filteredData.containsKey(mockAnimeDetailComplement2))
            assertEquals(listOf(mockEpisodeDetailComplement2), filteredData[mockAnimeDetailComplement2])
            assertEquals(1, pagination.items.total)
            assertEquals(1, pagination.items.count)
        }
        coVerify(exactly = 2) { repository.getAllEpisodeHistory(any()) }
        coVerify(exactly = 1) { AnimeTitleFinder.searchTitle<EpisodeDetailComplement>("Boruto", any(), any()) }
    }

    @Test
    fun `ChangePage updates page and fetches correct episodes`() = runTest {
        val episodes = List(15) { i ->
            mockEpisodeDetailComplement.copy(
                id = "ep_$i",
                malId = 1,
                episodeTitle = "Episode ${i + 1}",
                number = i + 1,
                lastWatched = "2025-05-12T23:54:02.306274",
                lastTimestamp = 600L - i
            )
        }
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(episodes)
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.ChangePage(2))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertEquals(2, queryState.page)
            assertTrue(filteredEpisodeHistoryResults is Resource.Success)
            assertTrue(episodeHistoryResults is Resource.Success)
            val filteredResults = (filteredEpisodeHistoryResults as Resource.Success).data
            val episodeResults = (episodeHistoryResults as Resource.Success).data
            val pageEpisodes = filteredResults[mockAnimeDetailComplement] ?: emptyList()
            assertEquals(5, pageEpisodes.size)
            assertEquals("Episode 11", pageEpisodes.first().episodeTitle)
            assertEquals(episodes, episodeResults)
            assertEquals(15, pagination.items.total)
            assertEquals(5, pagination.items.count)
            assertEquals(2, pagination.last_visible_page)
            assertFalse(pagination.has_next_page)
        }
        coVerify(exactly = 1) { repository.getAllEpisodeHistory(any()) }
    }

    @Test
    fun `ToggleEpisodeFavorite updates episode favorite status and preserves pagination`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2)
        )
        coEvery { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns mockEpisodeDetailComplement
        coEvery { repository.updateEpisodeDetailComplement(any()) } just Runs
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 2)
        } returns mockAnimeDetailComplement2

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.ChangePage(1))
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.ToggleEpisodeFavorite("lorem-ipsum-123?ep=123", true))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Success)
            assertTrue(episodeHistoryResults is Resource.Success)
            assertEquals(1, pagination.current_page)
            assertEquals(2, pagination.items.total)
            assertEquals(2, pagination.items.count)
        }
        coVerify(exactly = 1) { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
        coVerify(exactly = 1) { repository.updateEpisodeDetailComplement(any()) }
    }

    @Test
    fun `ToggleEpisodeFavorite with isFavorite filter removes episode if not matching`() = runTest {
        val queryState = EpisodeHistoryQueryState(isFavorite = true, page = 1, limit = 10)
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2)
        )
        coEvery { repository.getCachedEpisodeDetailComplement("lorem-ipsum-124?ep=124") } returns mockEpisodeDetailComplement2
        coEvery { repository.updateEpisodeDetailComplement(any()) } just Runs
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 2)
        } returns mockAnimeDetailComplement2

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.ApplyFilters(queryState))
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.ToggleEpisodeFavorite("lorem-ipsum-124?ep=124", false))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Success)
            assertTrue(episodeHistoryResults is Resource.Success)
            assertEquals(1, pagination.last_visible_page)
        }
        coVerify(exactly = 1) { repository.getCachedEpisodeDetailComplement("lorem-ipsum-124?ep=124") }
        coVerify(exactly = 1) { repository.updateEpisodeDetailComplement(any()) }
    }

    @Test
    fun `ToggleEpisodeFavorite multiple times persists all changes`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2)
        )
        coEvery { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns mockEpisodeDetailComplement
        coEvery { repository.getCachedEpisodeDetailComplement("lorem-ipsum-124?ep=124") } returns mockEpisodeDetailComplement2
        coEvery { repository.updateEpisodeDetailComplement(any()) } just Runs
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 2)
        } returns mockAnimeDetailComplement2

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.ToggleEpisodeFavorite("lorem-ipsum-123?ep=123", true))
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.ToggleEpisodeFavorite("lorem-ipsum-124?ep=124", false))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Success)
            assertTrue(episodeHistoryResults is Resource.Success)
            assertEquals(2, pagination.items.total)
        }
        coVerify(exactly = 1) { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
        coVerify(exactly = 1) { repository.getCachedEpisodeDetailComplement("lorem-ipsum-124?ep=124") }
        coVerify(exactly = 2) { repository.updateEpisodeDetailComplement(any()) }
    }

    @Test
    fun `ToggleEpisodeFavorite with error does not update state`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns null
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.ToggleEpisodeFavorite("lorem-ipsum-123?ep=123", true))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Error)
            assertTrue(episodeHistoryResults is Resource.Error)
            assertEquals("Episode not found", (filteredEpisodeHistoryResults as Resource.Error).message)
            assertEquals("Episode not found", (episodeHistoryResults as Resource.Error).message)
        }
        coVerify(exactly = 1) { repository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
        coVerify(exactly = 0) { repository.updateEpisodeDetailComplement(any()) }
    }

    @Test
    fun `ToggleAnimeFavorite updates anime favorite status and preserves pagination`() = runTest {
        val episodes = List(15) { i ->
            mockEpisodeDetailComplement.copy(
                id = "ep_$i",
                malId = 1,
                episodeTitle = "Episode ${i + 1}",
                number = i + 1
            )
        }
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(episodes)
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
        coEvery { repository.updateCachedAnimeDetailComplement(any()) } just Runs
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.ChangePage(2))
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.ToggleAnimeFavorite(1, true))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Success)
            assertTrue(episodeHistoryResults is Resource.Success)
            val filteredResults = (filteredEpisodeHistoryResults as Resource.Success).data
            assertEquals(2, pagination.current_page)
            assertEquals(5, filteredResults.values.sumOf { it.size })
            assertEquals(15, pagination.items.total)
            assertEquals(5, pagination.items.count)
        }
        coVerify(exactly = 1) { repository.getCachedAnimeDetailComplementByMalId(1) }
        coVerify(exactly = 1) { repository.updateCachedAnimeDetailComplement(any()) }
    }

    @Test
    fun `ToggleAnimeFavorite with error updates state with error`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.getCachedAnimeDetailComplementByMalId(1) } returns null
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.ToggleAnimeFavorite(1, true))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Error)
            assertTrue(episodeHistoryResults is Resource.Error)
            assertEquals("Anime not found", (filteredEpisodeHistoryResults as Resource.Error).message)
            assertEquals("Anime not found", (episodeHistoryResults as Resource.Error).message)
        }
        coVerify(exactly = 1) { repository.getCachedAnimeDetailComplementByMalId(1) }
        coVerify(exactly = 0) { repository.updateCachedAnimeDetailComplement(any()) }
    }

    @Test
    fun `DeleteEpisode removes episode using removeEpisodesFromFilteredMap`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2)
        )
        coEvery { repository.deleteEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns true
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 2)
        } returns mockAnimeDetailComplement2

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.DeleteEpisode("lorem-ipsum-123?ep=123"))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Success)
            assertTrue(episodeHistoryResults is Resource.Success)
            val filteredResults = (filteredEpisodeHistoryResults as Resource.Success).data
            assertTrue(filteredResults[mockAnimeDetailComplement2]?.any { it.id == "lorem-ipsum-124?ep=124" } == true)
            assertEquals(listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2), (episodeHistoryResults as Resource.Success).data)
        }
        coVerify(exactly = 1) { repository.deleteEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
    }

    @Test
    fun `DeleteEpisode with error updates state with error`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.deleteEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns false
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.DeleteEpisode("lorem-ipsum-123?ep=123"))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Error)
            assertTrue(episodeHistoryResults is Resource.Error)
            assertEquals("Episode not found", (filteredEpisodeHistoryResults as Resource.Error).message)
            assertEquals("Episode not found", (episodeHistoryResults as Resource.Error).message)
        }
        coVerify(exactly = 1) { repository.deleteEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
    }

    @Test
    fun `DeleteAnime removes episodes for anime using removeEpisodesFromFilteredMap`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2)
        )
        coEvery { repository.deleteAnimeDetailById(1) } just Runs
        coEvery { repository.deleteAnimeDetailComplement(1) } returns true
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 2)
        } returns mockAnimeDetailComplement2

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.DeleteAnime(1))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Success)
            assertTrue(episodeHistoryResults is Resource.Success)
            val filteredResults = (filteredEpisodeHistoryResults as Resource.Success).data
            assertTrue(filteredResults[mockAnimeDetailComplement2]?.any { it.id == "lorem-ipsum-124?ep=124" } == true)
            assertEquals(listOf(mockEpisodeDetailComplement, mockEpisodeDetailComplement2), (episodeHistoryResults as Resource.Success).data)
        }
        coVerify(exactly = 1) { repository.deleteAnimeDetailById(1) }
        coVerify(exactly = 1) { repository.deleteAnimeDetailComplement(1) }
    }

    @Test
    fun `DeleteAnime with error updates state with error`() = runTest {
        coEvery { repository.getAllEpisodeHistory(any()) } returns Resource.Success(
            listOf(mockEpisodeDetailComplement)
        )
        coEvery { repository.deleteAnimeDetailById(1) } just Runs
        coEvery { repository.deleteAnimeDetailComplement(1) } returns false
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(repository = repository, malId = 1)
        } returns mockAnimeDetailComplement

        viewModel = EpisodeHistoryViewModel(repository)
        viewModel.onAction(EpisodeHistoryAction.FetchHistory)
        advanceUntilIdle()
        viewModel.onAction(EpisodeHistoryAction.DeleteAnime(1))
        advanceUntilIdle()

        with(viewModel.historyState.value) {
            assertTrue(filteredEpisodeHistoryResults is Resource.Error)
            assertTrue(episodeHistoryResults is Resource.Error)
            assertEquals("Anime not found", (filteredEpisodeHistoryResults as Resource.Error).message)
            assertEquals("Anime not found", (episodeHistoryResults as Resource.Error).message)
        }
        coVerify(exactly = 1) { repository.deleteAnimeDetailById(1) }
        coVerify(exactly = 1) { repository.deleteAnimeDetailComplement(1) }
    }
}