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
        coEvery { repository.getGenres() } returns Resource.Success(mockk())
        coEvery { repository.getProducers(any()) } returns Resource.Success(mockk())
        coEvery { repository.searchAnime(any()) } returns Resource.Success(mockk())
        coEvery { repository.getRandomAnime() } returns Resource.Success(mockk())
        viewModel = AnimeSearchViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `searchAnime with empty query and default filters should call getRandomAnime`() = runTest {
        val mockAnimeDetailResponse = mockk<AnimeSearchResponse> {
            every { data } returns mockk()
        }

        coEvery { repository.getRandomAnime() } returns Resource.Success(mockAnimeDetailResponse)

        viewModel.searchAnime()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.getRandomAnime() }
    }

    @Test
    fun `searchAnime with query should call searchAnime from repository`() = runTest {
        viewModel.applyFilters(viewModel.queryState.value.copy(query = "Naruto"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.searchAnime(viewModel.queryState.value) }
    }

    @Test
    fun `fetchGenres should update genres state`() = runTest {
        val genres = listOf(
            Genre(mal_id = 1, name = "Action", url = "action.com", count = 100),
            Genre(mal_id = 2, name = "Adventure", url = "adventure.com", count = 200)
        )
        val genresResponse = GenresResponse(genres)

        coEvery { repository.getGenres() } returns Resource.Success(genresResponse)

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
    }

    @Test
    fun `applyGenreFilters should update queryState and call searchAnime`() = runTest {
        val genre = genrePlaceholder.copy(mal_id = 1)
        viewModel.setSelectedGenre(genre)
        val searchResponse =
            AnimeSearchResponse(
                data = emptyList(),
                pagination = defaultCompletePagination
            )
        coEvery { repository.searchAnime(any()) } returns Resource.Success(searchResponse)

        viewModel.applyGenreFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.animeSearchResults.first()
        assertEquals("1", viewModel.queryState.value.genres)
        assertEquals(Resource.Success(searchResponse), result)
    }

    @Test
    fun `resetGenreSelection should clear selected genres and reset queryState`() = runTest {
        val genre = genrePlaceholder

        val mockAnimeDetailResponse = mockk<AnimeSearchResponse> {
            every { data } returns mockk()
        }
        coEvery { repository.getRandomAnime() } returns Resource.Success(mockAnimeDetailResponse)

        viewModel.setSelectedGenre(genre)
        viewModel.applyGenreFilters()

        viewModel.resetGenreSelection()

        assertEquals(emptyList<Genre>(), viewModel.selectedGenres.value)
        assertEquals(null, viewModel.queryState.value.genres)
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
    }

    @Test
    fun `applyProducerFilters should update queryState and call searchAnime`() = runTest {
        val producer = producerPlaceholder.copy(mal_id = 1)
        viewModel.setSelectedProducer(producer)
        val searchResponse =
            AnimeSearchResponse(
                data = emptyList(),
                pagination = defaultCompletePagination
            )
        coEvery { repository.searchAnime(any()) } returns Resource.Success(searchResponse)

        viewModel.applyProducerFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.animeSearchResults.first()
        assertEquals("1", viewModel.queryState.value.producers)
        assertEquals(Resource.Success(searchResponse), result)
    }

    @Test
    fun `resetProducerSelection should clear selected producers and reset queryState`() = runTest {
        val producer = producerPlaceholder

        val mockAnimeDetailResponse = mockk<AnimeSearchResponse> {
            every { data } returns mockk()
        }

        coEvery { repository.getRandomAnime() } returns Resource.Success(mockAnimeDetailResponse)

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