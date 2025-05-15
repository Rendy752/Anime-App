package com.luminoverse.animevibe.animeRecommendations

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.luminoverse.animevibe.models.AnimeRecommendationResponse
import com.luminoverse.animevibe.models.defaultPagination
import com.luminoverse.animevibe.repository.AnimeRecommendationsRepository
import com.luminoverse.animevibe.ui.animeRecommendations.AnimeRecommendationsViewModel
import com.luminoverse.animevibe.ui.animeRecommendations.RecommendationsAction
import com.luminoverse.animevibe.ui.animeRecommendations.RecommendationsState
import com.luminoverse.animevibe.utils.Resource
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

@ExperimentalCoroutinesApi
class AnimeRecommendationsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val repository: AnimeRecommendationsRepository = mockk()
    private lateinit var viewModel: AnimeRecommendationsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        coEvery { repository.getAnimeRecommendations(1) } returns Resource.Loading()
        viewModel = AnimeRecommendationsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearMocks(repository)
    }

    @Test
    fun `Initial state and LoadRecommendations onAction`() = runTest {
        val initialState = RecommendationsState()

        assertTrue(viewModel.recommendationsState.value.animeRecommendations is Resource.Loading)
        assertEquals(initialState.isRefreshing, viewModel.recommendationsState.value.isRefreshing)
        coVerify(exactly = 1) { repository.getAnimeRecommendations(1) }
    }

    @Test
    fun `LoadRecommendations success`() = runTest {
        val mockResponse = Resource.Success(
            AnimeRecommendationResponse(
                pagination = defaultPagination,
                data = emptyList()
            )
        )
        clearMocks(repository)
        coEvery { repository.getAnimeRecommendations(1) } returns mockResponse

        viewModel.onAction(RecommendationsAction.LoadRecommendations)
        advanceUntilIdle()

        assertFalse(viewModel.recommendationsState.value.isRefreshing)
        assertEquals(mockResponse, viewModel.recommendationsState.value.animeRecommendations)
        coVerify(exactly = 1) { repository.getAnimeRecommendations(1) }
    }

    @Test
    fun `LoadRecommendations failure`() = runTest {
        val errorMessage = "Internal Server Error"
        val mockResponse = Resource.Error<AnimeRecommendationResponse>(errorMessage)
        clearMocks(repository)
        coEvery { repository.getAnimeRecommendations(1) } returns mockResponse

        viewModel.onAction(RecommendationsAction.LoadRecommendations)
        advanceUntilIdle()

        assertFalse(viewModel.recommendationsState.value.isRefreshing)
        assertEquals(mockResponse, viewModel.recommendationsState.value.animeRecommendations)
        assertNull(viewModel.recommendationsState.value.animeRecommendations.data)
        coVerify(exactly = 1) { repository.getAnimeRecommendations(1) }
    }

    @Test
    fun `Edge case - Multiple LoadRecommendations calls`() = runTest {
        val mockResponse = Resource.Success(
            AnimeRecommendationResponse(
                pagination = defaultPagination,
                data = emptyList()
            )
        )

        clearMocks(repository)
        coEvery { repository.getAnimeRecommendations(1) } returns mockResponse

        viewModel.onAction(RecommendationsAction.LoadRecommendations)
        viewModel.onAction(RecommendationsAction.LoadRecommendations)
        viewModel.onAction(RecommendationsAction.LoadRecommendations)

        advanceUntilIdle()

        assertFalse(viewModel.recommendationsState.value.isRefreshing)
        assertEquals(mockResponse, viewModel.recommendationsState.value.animeRecommendations)
        coVerify(exactly = 3) { repository.getAnimeRecommendations(1) }
    }

    @Test
    fun `Edge case - LoadRecommendations with empty repository response`() = runTest {
        val mockResponse = Resource.Success(
            AnimeRecommendationResponse(
                pagination = defaultPagination,
                data = emptyList()
            )
        )
        clearMocks(repository)
        coEvery { repository.getAnimeRecommendations(1) } returns mockResponse

        viewModel.onAction(RecommendationsAction.LoadRecommendations)
        advanceUntilIdle()

        assertFalse(viewModel.recommendationsState.value.isRefreshing)
        assertEquals(mockResponse, viewModel.recommendationsState.value.animeRecommendations)
        assertTrue(viewModel.recommendationsState.value.animeRecommendations.data?.data?.isEmpty() == true)
        coVerify(exactly = 1) { repository.getAnimeRecommendations(1) }
    }
}