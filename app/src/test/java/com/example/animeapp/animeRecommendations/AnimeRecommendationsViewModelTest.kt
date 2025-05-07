package com.example.animeapp.animeRecommendations

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.animeapp.models.AnimeRecommendationResponse
import com.example.animeapp.models.defaultPagination
import com.example.animeapp.repository.AnimeRecommendationsRepository
import com.example.animeapp.ui.animeRecommendations.AnimeRecommendationsViewModel
import com.example.animeapp.ui.animeRecommendations.RecommendationsAction
import com.example.animeapp.utils.Resource
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
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeRecommendationsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var repository: AnimeRecommendationsRepository

    private lateinit var viewModel: AnimeRecommendationsViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadAnimeRecommendations error`() = runTest {
        `when`(repository.getAnimeRecommendations()).thenReturn(
            Resource.Error("Internal Server Error")
        )

        viewModel = AnimeRecommendationsViewModel(repository)
        viewModel.onAction(RecommendationsAction.LoadRecommendations)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.recommendationsState.first()
        assert(state.animeRecommendations is Resource.Error)
        assertEquals("Internal Server Error", (state.animeRecommendations as Resource.Error).message)
        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `refreshing state is updated correctly`() = runTest {
        val mockResponse =
            AnimeRecommendationResponse(data = emptyList(), pagination = defaultPagination)
        `when`(repository.getAnimeRecommendations()).thenReturn(Resource.Success(mockResponse))

        viewModel = AnimeRecommendationsViewModel(repository)
        viewModel.onAction(RecommendationsAction.LoadRecommendations)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.recommendationsState.first()
        assert(state.animeRecommendations is Resource.Success)
        assertEquals(mockResponse, (state.animeRecommendations as Resource.Success).data)
        assertEquals(false, state.isRefreshing)
    }
}