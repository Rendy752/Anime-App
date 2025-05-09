package com.example.animeapp.animeSearch

import com.example.animeapp.models.AnimeSearchResponse
import com.example.animeapp.models.Genre
import com.example.animeapp.models.GenresResponse
import com.example.animeapp.models.JpgImage
import com.example.animeapp.models.Producer
import com.example.animeapp.models.ProducerImage
import com.example.animeapp.models.ProducersResponse
import com.example.animeapp.models.Title
import com.example.animeapp.models.defaultCompletePagination
import com.example.animeapp.models.genrePlaceholder
import com.example.animeapp.models.producerPlaceholder
import com.example.animeapp.repository.AnimeSearchRepository
import com.example.animeapp.ui.animeSearch.AnimeSearchViewModel
import com.example.animeapp.ui.animeSearch.SearchAction
import com.example.animeapp.utils.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeSearchViewModelTest {

    private lateinit var viewModel: AnimeSearchViewModel
    private val repository: AnimeSearchRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getGenres() } returns Resource.Success(GenresResponse(listOf()))
        coEvery { repository.getProducers(any()) } returns Resource.Success(ProducersResponse(defaultCompletePagination, listOf()))
        coEvery { repository.searchAnime(any()) } returns Resource.Success(AnimeSearchResponse(emptyList(), defaultCompletePagination))
        coEvery { repository.getRandomAnime() } returns Resource.Success(AnimeSearchResponse(emptyList(), defaultCompletePagination))
        viewModel = AnimeSearchViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `searchAnime with empty query and default filters should call getRandomAnime`() = runTest {
        val mockAnimeDetailResponse = mockk<AnimeSearchResponse> {
            every { data } returns emptyList()
            every { pagination } returns defaultCompletePagination
        }

        coEvery { repository.getRandomAnime() } returns Resource.Success(mockAnimeDetailResponse)

        viewModel.onAction(SearchAction.SearchAnime)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.getRandomAnime() }
        val state = viewModel.searchState.first()
        assertEquals(Resource.Success(mockAnimeDetailResponse), state.animeSearchResults)
    }

    @Test
    fun `searchAnime with query should call searchAnime from repository`() = runTest {
        val queryState = viewModel.searchState.value.queryState.copy(query = "Naruto")
        viewModel.onAction(SearchAction.ApplyFilters(queryState))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.searchAnime(queryState) }
    }

    @Test
    fun `fetchGenres should update genres state`() = runTest {
        val genres = listOf(
            Genre(mal_id = 1, name = "Action", url = "action.com", count = 100),
            Genre(mal_id = 2, name = "Adventure", url = "adventure.com", count = 200)
        )
        val genresResponse = GenresResponse(genres)

        coEvery { repository.getGenres() } returns Resource.Success(genresResponse)

        viewModel.onAction(SearchAction.FetchGenres)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.searchState.first()
        assertEquals(Resource.Success(genresResponse), state.genres)
    }

    @Test
    fun `setSelectedGenre should update selected genres`() = runTest {
        val genre = genrePlaceholder

        viewModel.onAction(SearchAction.SetSelectedGenre(genre))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.filterSelectionState.first()
        assertEquals(listOf(genre), state.selectedGenres)
    }

    @Test
    fun `applyGenreFilters should update queryState and call searchAnime`() = runTest {
        val genre = genrePlaceholder.copy(mal_id = 1)
        viewModel.onAction(SearchAction.SetSelectedGenre(genre))
        val searchResponse = AnimeSearchResponse(
            data = emptyList(),
            pagination = defaultCompletePagination
        )
        coEvery { repository.searchAnime(any()) } returns Resource.Success(searchResponse)

        viewModel.onAction(SearchAction.ApplyGenreFilters)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.searchState.first()
        assertEquals("1", state.queryState.genres)
        assertEquals(Resource.Success(searchResponse), state.animeSearchResults)
    }

    @Test
    fun `resetGenreSelection should clear selected genres and reset queryState`() = runTest {
        val genre = genrePlaceholder
        viewModel.onAction(SearchAction.SetSelectedGenre(genre))
        viewModel.onAction(SearchAction.ApplyGenreFilters)

        val mockAnimeDetailResponse = mockk<AnimeSearchResponse> {
            every { data } returns emptyList()
            every { pagination } returns defaultCompletePagination
        }
        coEvery { repository.getRandomAnime() } returns Resource.Success(mockAnimeDetailResponse)

        viewModel.onAction(SearchAction.ResetGenreSelection)
        testDispatcher.scheduler.advanceUntilIdle()

        val searchState = viewModel.searchState.first()
        val filterState = viewModel.filterSelectionState.first()
        assertEquals(emptyList<Genre>(), filterState.selectedGenres)
        assertEquals(null, searchState.queryState.genres)
    }

    @Test
    fun `fetchProducers should update producers state`() = runTest {
        val producers = listOf(
            Producer(
                mal_id = 1,
                url = "producer1.com",
                titles = listOf(Title(type = "Japanese", title = "Producer 1")),
                images = ProducerImage(jpg = JpgImage(image_url = "image1.com")),
                favorites = 100,
                established = "2000",
                about = "About Producer 1",
                count = 10
            ),
            Producer(
                mal_id = 2,
                url = "producer2.com",
                titles = listOf(Title(type = "Japanese", title = "Producer 2")),
                images = ProducerImage(jpg = JpgImage(image_url = "image2.com")),
                favorites = 200,
                established = "2010",
                about = "About Producer 2",
                count = 20
            )
        )
        val producersResponse = ProducersResponse(
            pagination = defaultCompletePagination,
            data = producers
        )

        coEvery { repository.getProducers(any()) } returns Resource.Success(producersResponse)

        viewModel.onAction(SearchAction.FetchProducers)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.searchState.first()
        assertEquals(Resource.Success(producersResponse), state.producers)
    }

    @Test
    fun `setSelectedProducer should update selected producers`() = runTest {
        val producer = producerPlaceholder

        viewModel.onAction(SearchAction.SetSelectedProducer(producer))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.filterSelectionState.first()
        assertEquals(listOf(producer), state.selectedProducers)
    }

    @Test
    fun `applyProducerFilters should update queryState and call searchAnime`() = runTest {
        val producer = producerPlaceholder.copy(mal_id = 1)
        viewModel.onAction(SearchAction.SetSelectedProducer(producer))
        val searchResponse = AnimeSearchResponse(
            data = emptyList(),
            pagination = defaultCompletePagination
        )
        coEvery { repository.searchAnime(any()) } returns Resource.Success(searchResponse)

        viewModel.onAction(SearchAction.ApplyProducerFilters)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.searchState.first()
        assertEquals("1", state.queryState.producers)
        assertEquals(Resource.Success(searchResponse), state.animeSearchResults)
    }

    @Test
    fun `resetProducerSelection should clear selected producers and reset queryState`() = runTest {
        val producer = producerPlaceholder
        viewModel.onAction(SearchAction.SetSelectedProducer(producer))
        viewModel.onAction(SearchAction.ApplyProducerFilters)

        val mockAnimeDetailResponse = mockk<AnimeSearchResponse> {
            every { data } returns emptyList()
            every { pagination } returns defaultCompletePagination
        }
        coEvery { repository.getRandomAnime() } returns Resource.Success(mockAnimeDetailResponse)

        viewModel.onAction(SearchAction.ResetProducerSelection)
        testDispatcher.scheduler.advanceUntilIdle()

        val searchState = viewModel.searchState.first()
        val filterState = viewModel.filterSelectionState.first()
        assertEquals(emptyList<Producer>(), filterState.selectedProducers)
        assertEquals(null, searchState.queryState.producers)
    }

    @Test
    fun `resetBottomSheetFilters should reset queryState and call searchAnime`() = runTest {
        val initialQueryState = viewModel.searchState.value.queryState.copy(
            query = "Naruto", genres = "1",
            producers = "1", page = 1, limit = 10, maxScore = 10.0, minScore = 5.0, type = "TV"
        )
        viewModel.onAction(SearchAction.ApplyFilters(initialQueryState))

        viewModel.onAction(SearchAction.ResetBottomSheetFilters)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.searchState.first()
        assertEquals(initialQueryState.resetBottomSheetFilters(), state.queryState)
        coVerify { repository.searchAnime(initialQueryState.resetBottomSheetFilters()) }
    }
}