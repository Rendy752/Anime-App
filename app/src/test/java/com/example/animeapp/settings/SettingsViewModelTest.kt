package com.example.animeapp.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.animeapp.models.AnimeSearchResponse
import com.example.animeapp.models.animeDetailPlaceholder
import com.example.animeapp.models.defaultCompletePagination
import com.example.animeapp.repository.AnimeSearchRepository
import com.example.animeapp.ui.settings.SettingsAction
import com.example.animeapp.ui.settings.SettingsViewModel
import com.example.animeapp.utils.Resource
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: SettingsViewModel
    private lateinit var animeSearchRepository: AnimeSearchRepository
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        animeSearchRepository = mockk()

        coEvery { animeSearchRepository.getRandomAnime() } returns Resource.Success(
            AnimeSearchResponse(
                data = listOf(animeDetailPlaceholder),
                pagination = defaultCompletePagination
            )
        )

        viewModel = SettingsViewModel(animeSearchRepository)
        runTest { advanceUntilIdle() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should trigger GetRandomAnime and update state with success response`() = runTest {
        val animeDetail = animeDetailPlaceholder
        clearMocks(animeSearchRepository, answers = false)
        coEvery { animeSearchRepository.getRandomAnime() } returns Resource.Success(
            AnimeSearchResponse(data = listOf(animeDetail), pagination = defaultCompletePagination)
        )

        viewModel.onAction(SettingsAction.GetRandomAnime)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(
            "animeDetailSample should be Success",
            state.animeDetailSample is Resource.Success
        )
        assertEquals(animeDetail, (state.animeDetailSample as Resource.Success).data)
        coVerify(exactly = 1) { animeSearchRepository.getRandomAnime() }
    }

    @Test
    fun `init should trigger GetRandomAnime and fallback to placeholder on error`() = runTest {
        clearMocks(animeSearchRepository, answers = false)
        coEvery { animeSearchRepository.getRandomAnime() } returns Resource.Error("Network error")

        viewModel.onAction(SettingsAction.GetRandomAnime)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(
            "animeDetailSample should be Success",
            state.animeDetailSample is Resource.Success
        )
        assertEquals(animeDetailPlaceholder, (state.animeDetailSample as Resource.Success).data)
        coVerify(exactly = 1) { animeSearchRepository.getRandomAnime() }
    }

    @Test
    fun `onAction GetRandomAnime should update state with success response`() = runTest {
        val animeDetail = animeDetailPlaceholder
        clearMocks(animeSearchRepository, answers = false)
        coEvery { animeSearchRepository.getRandomAnime() } returns Resource.Success(
            AnimeSearchResponse(
                data = listOf(animeDetailPlaceholder),
                pagination = defaultCompletePagination
            )
        )

        viewModel.onAction(SettingsAction.GetRandomAnime)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(
            "animeDetailSample should be Success",
            state.animeDetailSample is Resource.Success
        )
        assertEquals(animeDetail, (state.animeDetailSample as Resource.Success).data)
        coVerify(exactly = 1) { animeSearchRepository.getRandomAnime() }
    }

    @Test
    fun `onAction GetRandomAnime should fallback to placeholder on error`() = runTest {
        clearMocks(animeSearchRepository, answers = false)
        coEvery { animeSearchRepository.getRandomAnime() } returns Resource.Error("Network error")

        viewModel.onAction(SettingsAction.GetRandomAnime)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(
            "animeDetailSample should be Success",
            state.animeDetailSample is Resource.Success
        )
        assertEquals(animeDetailPlaceholder, (state.animeDetailSample as Resource.Success).data)
        coVerify(exactly = 1) { animeSearchRepository.getRandomAnime() }
    }
}