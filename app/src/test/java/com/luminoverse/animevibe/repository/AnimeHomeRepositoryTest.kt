package com.luminoverse.animevibe.repository

import com.luminoverse.animevibe.data.remote.api.AnimeAPI
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.utils.Resource
import com.luminoverse.animevibe.utils.TimeUtils
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.ZonedDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeHomeRepositoryTest {

    private lateinit var repository: AnimeHomeRepository
    private val animeApi: AnimeAPI = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = AnimeHomeRepository(animeApi)
        mockkObject(TimeUtils)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(TimeUtils)
        clearAllMocks()
    }

    @Test
    fun `getAnimeSchedules returns success with valid response`() = runTest {
        val queryState = AnimeSchedulesSearchQueryState(
            filter = "monday",
            sfw = true,
            kids = false,
            unapproved = false,
            page = 1,
            limit = 10
        )
        val broadcast1 = mockk<Broadcast>(relaxed = true) {
            every { time } returns "12:00"
            every { timezone } returns "Asia/Tokyo"
            every { day } returns "Monday"
        }
        val broadcast2 = mockk<Broadcast>(relaxed = true) {
            every { time } returns "14:00"
            every { timezone } returns "Asia/Tokyo"
            every { day } returns "Monday"
        }
        val anime1 = mockk<AnimeDetail>(relaxed = true) {
            every { mal_id } returns 1
            every { title } returns "Anime 1"
            every { broadcast } returns broadcast1
        }
        val anime2 = mockk<AnimeDetail>(relaxed = true) {
            every { mal_id } returns 2
            every { title } returns "Anime 2"
            every { broadcast } returns broadcast2
        }
        val pagination = mockk<CompletePagination>(relaxed = true) {
            every { has_next_page } returns false
        }
        val responseData = ListAnimeDetailResponse(
            data = listOf(anime1, anime2, anime1),
            pagination = pagination
        )
        coEvery {
            animeApi.getAnimeSchedules(
                filter = queryState.filter,
                sfw = queryState.sfw,
                kids = queryState.kids,
                unapproved = queryState.unapproved,
                page = queryState.page,
                limit = queryState.limit
            )
        } returns Response.success(responseData)
        coEvery {
            TimeUtils.getBroadcastDateTimeForSorting(
                broadcastTime = "12:00",
                broadcastTimezone = "Asia/Tokyo",
                broadcastDay = "Monday"
            )
        } returns ZonedDateTime.of(2025, 5, 12, 12, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
        coEvery {
            TimeUtils.getBroadcastDateTimeForSorting(
                broadcastTime = "14:00",
                broadcastTimezone = "Asia/Tokyo",
                broadcastDay = "Monday"
            )
        } returns ZonedDateTime.of(2025, 5, 12, 14, 0, 0, 0, ZoneId.of("Asia/Tokyo"))

        val result = repository.getAnimeSchedules(queryState)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Success) {
            println("Unexpected result: $result")
            if (result is Resource.Error) {
                println("Error message: ${result.message}")
            }
        }
        Assert.assertTrue(result is Resource.Success)
        val successResult = result as Resource.Success
        Assert.assertEquals(listOf(anime1, anime2), successResult.data.data)
        Assert.assertEquals(pagination, successResult.data.pagination)
        coVerify {
            animeApi.getAnimeSchedules(
                queryState.filter,
                queryState.sfw,
                queryState.kids,
                queryState.unapproved,
                queryState.page,
                queryState.limit
            )
        }
    }

    @Test
    fun `getAnimeSchedules returns error with exception`() = runTest {
        val queryState = AnimeSchedulesSearchQueryState(filter = "monday")
        coEvery {
            animeApi.getAnimeSchedules(any(), any(), any(), any(), any(), any())
        } throws RuntimeException("Network error")

        val result = repository.getAnimeSchedules(queryState)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Error) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Error)
        Assert.assertTrue((result as Resource.Error).message?.contains("Unknown error") == true)
        coVerify {
            animeApi.getAnimeSchedules(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `getTop10Anime returns success with valid response`() = runTest {
        val animeList = (1..15).map { id ->
            mockk<AnimeDetail>(relaxed = true) {
                every { mal_id } returns id
                every { title } returns "Anime $id"
            }
        }
        val pagination = mockk<CompletePagination>(relaxed = true) {
            every { has_next_page } returns true
        }
        val responseData = ListAnimeDetailResponse(
            data = listOf(
                animeList[0],
                animeList[1],
                animeList[0],
                *animeList.subList(2, 15).toTypedArray()
            ),
            pagination = pagination
        )
        coEvery { animeApi.getTop20Anime() } returns Response.success(responseData)

        val result = repository.getTop10Anime()
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Success) {
            println("Unexpected result: $result")
            if (result is Resource.Error) {
                println("Error message: ${result.message}")
            }
        }
        Assert.assertTrue(result is Resource.Success)
        val successResult = result as Resource.Success
        Assert.assertEquals(animeList.take(10), successResult.data.data)
        Assert.assertEquals(pagination, successResult.data.pagination)
        coVerify { animeApi.getTop20Anime() }
    }

    @Test
    fun `getTop10Anime returns error with exception`() = runTest {
        coEvery { animeApi.getTop20Anime() } throws RuntimeException("API error")

        val result = repository.getTop10Anime()
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Error) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Error)
        Assert.assertTrue((result as Resource.Error).message?.contains("Unknown error") == true)
        coVerify { animeApi.getTop20Anime() }
    }
}