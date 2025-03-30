package com.example.animeapp.animeRecommendations

import com.example.animeapp.ui.animeSearch.AnimeSearchViewModel

import com.example.animeapp.models.*
import com.example.animeapp.repository.AnimeSearchRepository
import com.example.animeapp.utils.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeSearchViewModelTest {

    private lateinit var viewModel: AnimeSearchViewModel
    private val repository: AnimeSearchRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getGenres() } returns Response.success(mockk())
        coEvery { repository.getProducers(any()) } returns Response.success(mockk())
        coEvery { repository.searchAnime(any()) } returns Response.success(mockk())
        coEvery { repository.getRandomAnime() } returns Response.success(mockk())
        viewModel = AnimeSearchViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `searchAnime with empty query and default filters should call getRandomAnime`() = runTest {
        coEvery { repository.getRandomAnime() } returns Response.success(mockk())

        viewModel.searchAnime()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.getRandomAnime() }
    }

    @Test
    fun `searchAnime with query should call searchAnime from repository`() = runTest {
        val searchQueryState = AnimeSearchQueryState(query = "Naruto")
        val searchResponse =
            AnimeSearchResponse(data = emptyList(), pagination = CompletePagination.default())
        coEvery { repository.searchAnime(searchQueryState) } returns Response.success(searchResponse)

        viewModel.applyFilters(searchQueryState)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.animeSearchResults.first()

        assertEquals(Resource.Success(searchResponse), result)
    }

    @Test
    fun `fetchGenres should update genres state`() = runTest {
        val genresResponse = GenresResponse(mockk())
        coEvery { repository.getGenres() } returns Response.success(genresResponse)

        viewModel.fetchGenres()
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.genres.first()

        assertEquals(Resource.Success(genresResponse), result)
    }

    @Test
    fun `setSelectedGenre should update selected genres`() = runTest {
        val genre = genrePlaceholder

        viewModel.setSelectedGenre(genre)

        assertEquals(listOf(genre), viewModel.selectedGenres.value)

        viewModel.setSelectedGenre(genre)
        assertEquals(emptyList<Genre>(), viewModel.selectedGenres.value)
    }

    @Test
    fun `applyGenreFilters should update queryState and call searchAnime`() = runTest {
        val genre = genrePlaceholder
        viewModel.setSelectedGenre(genre)
        val searchResponse =
            AnimeSearchResponse(data = emptyList(), pagination = CompletePagination.default())
        coEvery { repository.searchAnime(any()) } returns Response.success(searchResponse)

        viewModel.applyGenreFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.animeSearchResults.first()
        assertEquals("1", viewModel.queryState.value.genres)
        assertEquals(Resource.Success(searchResponse), result)
    }

    @Test
    fun `resetGenreSelection should clear selected genres and reset queryState`() = runTest {
        val genre = genrePlaceholder
        viewModel.setSelectedGenre(genre)
        viewModel.applyGenreFilters()

        viewModel.resetGenreSelection()

        assertEquals(emptyList<Genre>(), viewModel.selectedGenres.value)
        assertEquals(null, viewModel.queryState.value.genres)
    }

    @Test
    fun `fetchProducers should update producers state`() = runTest {
        val producersResponse =
            ProducersResponse(pagination = CompletePagination.default(), data = mockk())
        coEvery { repository.getProducers(any()) } returns Response.success(producersResponse)

        viewModel.fetchProducers()
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.producers.first()

        assertEquals(Resource.Success(producersResponse), result)
    }

    @Test
    fun `setSelectedProducer should update selected producers`() = runTest {
        val producer = producerPlaceholder

        viewModel.setSelectedProducer(producer)

        assertEquals(listOf(producer), viewModel.selectedProducers.value)

        viewModel.setSelectedProducer(producer)
        assertEquals(emptyList<Producer>(), viewModel.selectedProducers.value)
    }

    @Test
    fun `applyProducerFilters should update queryState and call searchAnime`() = runTest {
        val producer = producerPlaceholder
        viewModel.setSelectedProducer(producer)
        val searchResponse =
            AnimeSearchResponse(data = emptyList(), pagination = CompletePagination.default())
        coEvery { repository.searchAnime(any()) } returns Response.success(searchResponse)

        viewModel.applyProducerFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.animeSearchResults.first()
        assertEquals("1", viewModel.queryState.value.producers)
        assertEquals(Resource.Success(searchResponse), result)
    }

    @Test
    fun `resetProducerSelection should clear selected producers and reset queryState`() = runTest {
        val producer = producerPlaceholder
        viewModel.setSelectedProducer(producer)
        viewModel.applyProducerFilters()

        viewModel.resetProducerSelection()

        assertEquals(emptyList<Producer>(), viewModel.selectedProducers.value)
        assertEquals(null, viewModel.queryState.value.producers)
    }

    @Test
    fun `resetBottomSheetFilters should reset queryState and call searchAnime`() = runTest {
        val initialQueryState = viewModel.queryState.value.copy(
            query = "Naruto", genres = "1",
            producers = "1", page = 1, limit = 10, maxScore = 10.0, minScore = 5.0, type = "TV"
        )
        viewModel.applyFilters(initialQueryState)

        viewModel.resetBottomSheetFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(initialQueryState.resetBottomSheetFilters(), viewModel.queryState.value)
        coVerify { repository.searchAnime(initialQueryState.resetBottomSheetFilters()) }
    }
}