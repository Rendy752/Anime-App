package com.luminoverse.animevibe.animeSearch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.repository.AnimeSearchRepository
import com.luminoverse.animevibe.ui.animeSearch.AnimeSearchViewModel
import com.luminoverse.animevibe.ui.animeSearch.SearchAction
import com.luminoverse.animevibe.utils.Resource
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AnimeSearchViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AnimeSearchViewModel
    private lateinit var testDispatcher: TestDispatcher

    @MockK
    private lateinit var animeSearchRepository: AnimeSearchRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        coEvery { animeSearchRepository.getCachedGenres() } returns emptyList()
        coEvery { animeSearchRepository.getGenres() } returns Resource.Success(GenresResponse(data = emptyList()))
        coEvery { animeSearchRepository.getProducers(any()) } returns Resource.Success(
            ProducersResponse(data = emptyList(), pagination = defaultCompletePagination)
        )
        coEvery { animeSearchRepository.insertCachedGenre(any()) } just Runs
        viewModel = AnimeSearchViewModel(animeSearchRepository)
        runTest { advanceUntilIdle() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `init should fetch genres and producers`() = runTest {
        val genres = listOf(genrePlaceholder)
        val producers = listOf(producerPlaceholder)
        coEvery { animeSearchRepository.getCachedGenres() } returns emptyList()
        coEvery { animeSearchRepository.getGenres() } returns Resource.Success(GenresResponse(data = genres))
        coEvery { animeSearchRepository.getProducers(any()) } returns Resource.Success(
            ProducersResponse(data = producers, pagination = defaultCompletePagination)
        )
        coEvery { animeSearchRepository.insertCachedGenre(any()) } just Runs

        viewModel = AnimeSearchViewModel(animeSearchRepository)
        advanceUntilIdle()

        val state = viewModel.searchState.value
        assertEquals(Resource.Success(GenresResponse(data = genres)), state.genres)
        assertEquals(
            Resource.Success(
                ProducersResponse(
                    data = producers,
                    pagination = defaultCompletePagination
                )
            ),
            state.producers
        )
        coVerify { animeSearchRepository.getCachedGenres() }
        coVerify { animeSearchRepository.getGenres() }
        coVerify { animeSearchRepository.getProducers(any()) }
    }

    @Test
    fun `fetchGenres should use cached genres if available`() = runTest {
        val cachedGenres = listOf(genrePlaceholder)
        coEvery { animeSearchRepository.getCachedGenres() } returns cachedGenres
        coEvery { animeSearchRepository.getGenres() } returns Resource.Success(GenresResponse(data = emptyList()))
        coEvery { animeSearchRepository.insertCachedGenre(any()) } just Runs

        clearMocks(animeSearchRepository, answers = false)
        viewModel = AnimeSearchViewModel(animeSearchRepository)
        advanceUntilIdle()
        viewModel.onAction(SearchAction.FetchGenres)
        advanceUntilIdle()

        val state = viewModel.searchState.value
        assertEquals(Resource.Success(GenresResponse(data = cachedGenres)), state.genres)
        coVerify { animeSearchRepository.getCachedGenres() }
        coVerify(exactly = 0) { animeSearchRepository.getGenres() }
        coVerify(exactly = 0) { animeSearchRepository.insertCachedGenre(any()) }
    }

    @Test
    fun `handleInitialFetch with genreId should set genre and apply filters`() = runTest {
        val genreId = 1
        val genre = genrePlaceholder.copy(mal_id = genreId)
        val genres = listOf(genre)
        val searchResponse =
            AnimeSearchResponse(data = emptyList(), pagination = defaultCompletePagination)
        coEvery { animeSearchRepository.getGenres() } returns Resource.Success(GenresResponse(data = genres))
        coEvery { animeSearchRepository.searchAnime(any()) } returns Resource.Success(searchResponse)
        coEvery { animeSearchRepository.getRandomAnime() } returns Resource.Success(searchResponse)

        viewModel = AnimeSearchViewModel(animeSearchRepository)
        advanceUntilIdle()
        viewModel.onAction(SearchAction.HandleInitialFetch(genreId = genreId))
        advanceUntilIdle()

        val state = viewModel.searchState.value
        val filterState = viewModel.filterSelectionState.value
        assertEquals(listOf(genre), filterState.selectedGenres)
        assertEquals(Resource.Success(searchResponse), state.animeSearchResults)
        assertFalse(state.isRefreshing)
        assertEquals(genreId.toString(), state.queryState.genres)
        coVerify { animeSearchRepository.searchAnime(match { it.genres == genreId.toString() }) }
    }

    @Test
    fun `handleInitialFetch with producerId should set producer and apply filters`() = runTest {
        val producerId = 1
        val producer = producerPlaceholder.copy(mal_id = producerId)
        val searchResponse =
            AnimeSearchResponse(data = emptyList(), pagination = defaultCompletePagination)
        coEvery { animeSearchRepository.getProducer(producerId) } returns Resource.Success(
            ProducerResponse(data = producer)
        )
        coEvery { animeSearchRepository.searchAnime(any()) } returns Resource.Success(searchResponse)
        coEvery { animeSearchRepository.getRandomAnime() } returns Resource.Success(searchResponse)

        viewModel = AnimeSearchViewModel(animeSearchRepository)
        advanceUntilIdle()
        viewModel.onAction(SearchAction.HandleInitialFetch(producerId = producerId))
        advanceUntilIdle()

        val state = viewModel.searchState.value
        val filterState = viewModel.filterSelectionState.value
        assertEquals(listOf(producer), filterState.selectedProducers)
        assertEquals(Resource.Success(searchResponse), state.animeSearchResults)
        assertFalse(state.isRefreshing)
        assertEquals(producerId.toString(), state.queryState.producers)
        coVerify { animeSearchRepository.searchAnime(match { it.producers == producerId.toString() }) }
    }

    @Test
    fun `handleInitialFetch with no parameters should fetch random anime if no results`() =
        runTest {
            val searchResponse =
                AnimeSearchResponse(data = emptyList(), pagination = defaultCompletePagination)
            coEvery { animeSearchRepository.getRandomAnime() } returns Resource.Success(
                searchResponse
            )

            viewModel = AnimeSearchViewModel(animeSearchRepository)
            advanceUntilIdle()
            viewModel.onAction(SearchAction.HandleInitialFetch())
            advanceUntilIdle()

            val state = viewModel.searchState.value
            assertEquals(Resource.Success(searchResponse), state.animeSearchResults)
            assertFalse(state.isRefreshing)
            coVerify { animeSearchRepository.getRandomAnime() }
            coVerify(exactly = 0) { animeSearchRepository.searchAnime(any()) }
        }

    @Test
    fun `handleInitialFetch should not trigger again if already fetched`() = runTest {
        val searchResponse =
            AnimeSearchResponse(data = emptyList(), pagination = defaultCompletePagination)
        coEvery { animeSearchRepository.getRandomAnime() } returns Resource.Success(searchResponse)

        viewModel = AnimeSearchViewModel(animeSearchRepository)
        advanceUntilIdle()
        viewModel.onAction(SearchAction.HandleInitialFetch())
        advanceUntilIdle()
        viewModel.onAction(SearchAction.HandleInitialFetch(genreId = 1))
        advanceUntilIdle()

        coVerify(exactly = 1) { animeSearchRepository.getRandomAnime() }
        coVerify(exactly = 0) { animeSearchRepository.searchAnime(any()) }
    }

    @Test
    fun `resetBottomSheetFilters should reset specific queryState fields and call searchAnime`() = runTest {
        val initialQueryState = AnimeSearchQueryState(
            query = "test",
            genres = "1",
            producers = "2",
            page = 1,
            limit = 10,
            sfw = true,
            type = "tv",
            status = "airing"
        )
        val searchResponse = AnimeSearchResponse(data = emptyList(), pagination = defaultCompletePagination)
        coEvery { animeSearchRepository.searchAnime(any()) } returns Resource.Success(searchResponse)

        viewModel = AnimeSearchViewModel(animeSearchRepository)
        advanceUntilIdle()
        viewModel.onAction(SearchAction.ApplyFilters(initialQueryState))
        advanceUntilIdle()
        viewModel.onAction(SearchAction.ResetBottomSheetFilters)
        advanceUntilIdle()

        val state = viewModel.searchState.value
        val expectedQueryState = AnimeSearchQueryState(
            query = "test",
            genres = "1",
            producers = "2",
            page = 1,
            limit = 10,
            sfw = true,
            type = null,
            status = null,
            rating = null,
            score = null,
            minScore = null,
            maxScore = null,
            orderBy = null,
            sort = null,
            startDate = null,
            endDate = null,
            unapproved = null
        )
        assertEquals(expectedQueryState, state.queryState)
        assertEquals(Resource.Success(searchResponse), state.animeSearchResults)
        assertFalse(state.isRefreshing)
        coVerify {
            animeSearchRepository.searchAnime(
                match {
                    it.query == "test" && it.genres == "1" && it.producers == "2"
                }
            )
        }
    }

    @Test
    fun `setSelectedGenre should toggle genre in filterSelectionState`() = runTest {
        val genre = genrePlaceholder

        viewModel = AnimeSearchViewModel(animeSearchRepository)
        advanceUntilIdle()
        viewModel.onAction(SearchAction.SetSelectedGenre(genre))
        advanceUntilIdle()

        val filterState = viewModel.filterSelectionState.value
        assertEquals(listOf(genre), filterState.selectedGenres)

        viewModel.onAction(SearchAction.SetSelectedGenre(genre))
        advanceUntilIdle()

        assertEquals(emptyList<Genre>(), viewModel.filterSelectionState.value.selectedGenres)
    }

    @Test
    fun `applyGenreFilters should update queryState and call searchAnime`() = runTest {
        val genre = genrePlaceholder
        val genres = listOf(genre)
        val searchResponse =
            AnimeSearchResponse(data = emptyList(), pagination = defaultCompletePagination)
        coEvery { animeSearchRepository.getGenres() } returns Resource.Success(GenresResponse(data = genres))
        coEvery { animeSearchRepository.searchAnime(any()) } returns Resource.Success(searchResponse)

        viewModel = AnimeSearchViewModel(animeSearchRepository)
        advanceUntilIdle()
        viewModel.onAction(SearchAction.SetSelectedGenre(genre))
        advanceUntilIdle()
        viewModel.onAction(SearchAction.ApplyGenreFilters)
        advanceUntilIdle()

        val state = viewModel.searchState.value
        assertEquals("1", state.queryState.genres)
        assertEquals(Resource.Success(searchResponse), state.animeSearchResults)
        assertFalse(state.isRefreshing)
        coVerify(exactly = 1) { animeSearchRepository.searchAnime(match { it.genres == "1" }) }
    }
}