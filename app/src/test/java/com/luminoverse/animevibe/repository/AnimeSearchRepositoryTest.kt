package com.luminoverse.animevibe.repository

import com.luminoverse.animevibe.data.local.dao.GenreDao
import com.luminoverse.animevibe.data.remote.api.AnimeAPI
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.utils.Resource
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeSearchRepositoryTest {

    private lateinit var repository: AnimeSearchRepository
    private val animeApi: AnimeAPI = mockk()
    private val genreDao: GenreDao = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = AnimeSearchRepository(animeApi, genreDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `getAnimeSearch returns success with valid response`() = runTest {
        val queryState = AnimeSearchQueryState(
            query = "Naruto",
            page = 1,
            limit = 25
        )
        val anime = mockk<AnimeDetail>(relaxed = true) {
            every { mal_id } returns 1
            every { title } returns "Naruto"
        }
        val pagination = mockk<CompletePagination>(relaxed = true) {
            every { has_next_page } returns false
        }
        val responseData = AnimeSearchResponse(
            data = listOf(anime),
            pagination = pagination
        )
        coEvery {
            animeApi.getAnimeSearch(
                q = any(),
                page = any(),
                limit = any(),
                type = any(),
                score = any(),
                minScore = any(),
                maxScore = any(),
                status = any(),
                rating = any(),
                sfw = any(),
                unapproved = any(),
                genres = any(),
                genresExclude = any(),
                orderBy = any(),
                sort = any(),
                letter = any(),
                producers = any(),
                startDate = any(),
                endDate = any()
            )
        } returns Response.success(responseData)

        val result = repository.searchAnime(queryState)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Success) {
            println("Unexpected result: $result")
            if (result is Resource.Error) {
                println("Error message: ${result.message}")
            }
        }
        Assert.assertTrue(result is Resource.Success)
        Assert.assertEquals(responseData, (result as Resource.Success).data)
        coVerify {
            animeApi.getAnimeSearch(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `getAnimeSearch returns error with exception`() = runTest {
        val queryState = AnimeSearchQueryState(query = "Naruto")
        coEvery {
            animeApi.getAnimeSearch(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws RuntimeException("Network error")

        val result = repository.searchAnime(queryState)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Error) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Error)
        Assert.assertTrue((result as Resource.Error).message?.contains("Unknown error") == true)
        coVerify {
            animeApi.getAnimeSearch(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `getRandomAnime returns success with single anime`() = runTest {
        val animeData = mockk<AnimeDetail>()
        val responseData = AnimeDetailResponse(data = animeData)
        coEvery { animeApi.getRandomAnime(sfw = true) } returns Response.success(responseData)

        val result = repository.getRandomAnime()
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Success) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Success)
        val successResult = result as Resource.Success
        Assert.assertEquals(listOf(animeData), successResult.data.data)
        Assert.assertEquals(defaultCompletePagination, successResult.data.pagination)
        coVerify { animeApi.getRandomAnime(sfw = true) }
    }

    @Test
    fun `getRandomAnime returns error with exception`() = runTest {
        coEvery { animeApi.getRandomAnime(sfw = true) } throws RuntimeException("API error")

        val result = repository.getRandomAnime()
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Error) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Error)
        Assert.assertTrue((result as Resource.Error).message?.contains("Unknown error") == true)
        coVerify { animeApi.getRandomAnime(sfw = true) }
    }

    @Test
    fun `getGenres returns success with valid response`() = runTest {
        val responseData = GenresResponse(data = listOf(mockk()))
        coEvery { animeApi.getGenres() } returns Response.success(responseData)

        val result = repository.getGenres()
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Success) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Success)
        Assert.assertEquals(responseData, (result as Resource.Success).data)
        coVerify { animeApi.getGenres() }
    }

    @Test
    fun `getGenres returns error with exception`() = runTest {
        coEvery { animeApi.getGenres() } throws RuntimeException("Network error")

        val result = repository.getGenres()
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Error) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Error)
        Assert.assertTrue((result as Resource.Error).message?.contains("Unknown error") == true)
        coVerify { animeApi.getGenres() }
    }

    @Test
    fun `getProducers returns success with valid response`() = runTest {
        val queryState = ProducersSearchQueryState(
            query = "Studio",
            page = 1,
            limit = 10
        )
        val responseData = ProducersResponse(data = listOf(mockk()), pagination = mockk())
        coEvery {
            animeApi.getProducers(
                page = queryState.page,
                limit = queryState.limit,
                q = queryState.query,
                orderBy = null,
                sort = null,
                letter = null
            )
        } returns Response.success(responseData)

        val result = repository.getProducers(queryState)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Success) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Success)
        Assert.assertEquals(responseData, (result as Resource.Success).data)
        coVerify { animeApi.getProducers(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `getProducers returns error with exception`() = runTest {
        val queryState = ProducersSearchQueryState(query = "Studio")
        coEvery {
            animeApi.getProducers(any(), any(), any(), any(), any(), any())
        } throws RuntimeException("API error")

        val result = repository.getProducers(queryState)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Error) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Error)
        Assert.assertTrue((result as Resource.Error).message?.contains("Unknown error") == true)
        coVerify { animeApi.getProducers(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `getProducer returns success with valid response`() = runTest {
        val malId = 1
        val responseData = ProducerResponse(data = mockk())
        coEvery { animeApi.getProducer(malId) } returns Response.success(responseData)

        val result = repository.getProducer(malId)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Success) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Success)
        Assert.assertEquals(responseData, (result as Resource.Success).data)
        coVerify { animeApi.getProducer(malId) }
    }

    @Test
    fun `getProducer returns error with exception`() = runTest {
        val malId = 1
        coEvery { animeApi.getProducer(malId) } throws RuntimeException("Not found")

        val result = repository.getProducer(malId)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Error) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Error)
        Assert.assertTrue((result as Resource.Error).message?.contains("Unknown error") == true)
        coVerify { animeApi.getProducer(malId) }
    }

    @Test
    fun `getCachedGenres returns list from Dao`() = runTest {
        val genres = listOf(
            genrePlaceholder.copy(mal_id = 1, name = "Action"),
            genrePlaceholder.copy(mal_id = 2, name = "Adventure")
        )
        coEvery { genreDao.getGenres() } returns genres

        val result = repository.getCachedGenres()
        testDispatcher.scheduler.advanceUntilIdle()

        Assert.assertEquals(genres, result)
        coVerify { genreDao.getGenres() }
    }

    @Test
    fun `insertCachedGenre calls Dao insert`() = runTest {
        val genre = genrePlaceholder.copy(mal_id = 1, name = "Action")
        coEvery { genreDao.insertGenre(genre) } returns Unit

        repository.insertCachedGenre(genre)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { genreDao.insertGenre(genre) }
    }
}