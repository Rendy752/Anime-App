package com.luminoverse.animevibe.repository

import com.luminoverse.animevibe.data.remote.api.AnimeAPI
import com.luminoverse.animevibe.models.AnimeRecommendationResponse
import com.luminoverse.animevibe.utils.Resource
import com.luminoverse.animevibe.utils.ResponseHandler
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeRecommendationsRepositoryTest {

    private lateinit var repository: AnimeRecommendationsRepository
    private val jikanAPI: AnimeAPI = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(ResponseHandler)
        repository = AnimeRecommendationsRepository(jikanAPI)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(ResponseHandler)
        clearAllMocks()
    }

    @Test
    fun `getAnimeRecommendations returns success with valid response`() = runTest {
        val page = 1
        val recommendationResponse = mockk<AnimeRecommendationResponse>(relaxed = true)
        coEvery { jikanAPI.getAnimeRecommendations(page) } returns Response.success(
            recommendationResponse
        )
        coEvery { ResponseHandler.handleCommonResponse(any<Response<AnimeRecommendationResponse>>()) } returns Resource.Success(
            recommendationResponse
        )

        val result = repository.getAnimeRecommendations(page)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Success) {
            println("Unexpected result: $result")
            if (result is Resource.Error) {
                println("Error message: ${result.message}")
            }
        }
        Assert.assertTrue(result is Resource.Success)
        Assert.assertEquals(recommendationResponse, (result as Resource.Success).data)
        coVerify { jikanAPI.getAnimeRecommendations(page) }
    }

    @Test
    fun `getAnimeRecommendations returns error with response body`() = runTest {
        val page = 1
        val errorResponseBody = "{\"error\": \"API Error\"}".toResponseBody()
        coEvery { jikanAPI.getAnimeRecommendations(page) } returns Response.error(
            400,
            errorResponseBody
        )
        coEvery { ResponseHandler.handleCommonResponse(any<Response<AnimeRecommendationResponse>>()) } returns Resource.Error(
            "{\"error\": \"API Error\"}"
        )

        val result = repository.getAnimeRecommendations(page)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Error) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Error)
        Assert.assertEquals("{\"error\": \"API Error\"}", (result as Resource.Error).message)
        coVerify { jikanAPI.getAnimeRecommendations(page) }
    }

    @Test
    fun `getAnimeRecommendations returns error with exception`() = runTest {
        val page = 1
        coEvery { jikanAPI.getAnimeRecommendations(page) } throws RuntimeException("Network Error")
        coEvery { ResponseHandler.handleCommonResponse<AnimeRecommendationResponse>(any()) } returns Resource.Error(
            "Unknown error"
        )

        val result = repository.getAnimeRecommendations(page)
        testDispatcher.scheduler.advanceUntilIdle()

        if (result !is Resource.Error) {
            println("Unexpected result: $result")
        }
        Assert.assertTrue(result is Resource.Error)
        Assert.assertEquals("Unknown error", (result as Resource.Error).message)
        coVerify { jikanAPI.getAnimeRecommendations(page) }
    }
}