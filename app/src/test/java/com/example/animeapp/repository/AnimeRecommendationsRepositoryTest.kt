package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeRecommendationResponse
import com.example.animeapp.utils.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import retrofit2.Response

class AnimeRecommendationsRepositoryTest {

    @Mock
    private lateinit var jikanAPI: AnimeAPI

    private lateinit var repository: AnimeRecommendationsRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = AnimeRecommendationsRepository(jikanAPI)
    }

    @Test
    fun `getAnimeRecommendations returns error with response body`() = runBlocking {
        val errorResponseBody = "{\"error\": \"API Error\"}".toResponseBody()
        val errorResponse = Response.error<AnimeRecommendationResponse>(400, errorResponseBody)
        `when`(jikanAPI.getAnimeRecommendations(1)).thenReturn(errorResponse)

        val result = repository.getAnimeRecommendations(1)

        assert(result is Resource.Error)
        assertEquals("{\"error\": \"API Error\"}", (result as Resource.Error).message)
    }

    @Test
    fun `getAnimeRecommendations returns error with exception`() = runBlocking {
        `when`(jikanAPI.getAnimeRecommendations(1)).thenThrow(RuntimeException("Network Error"))

        val result = repository.getAnimeRecommendations(1)

        assert(result is Resource.Error)
        assertEquals("Unknown error", (result as Resource.Error).message)
    }
}