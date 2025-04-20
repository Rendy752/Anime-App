package com.example.animeapp.animeHome

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.animeapp.models.AnimeSchedulesResponse
import com.example.animeapp.models.AnimeSchedulesSearchQueryState
import com.example.animeapp.models.animeDetailPlaceholder
import com.example.animeapp.models.defaultCompletePagination
import com.example.animeapp.models.episodeDetailComplementPlaceholder
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.repository.AnimeHomeRepository
import com.example.animeapp.ui.animeHome.HomeAction
import com.example.animeapp.ui.animeHome.HomeState
import com.example.animeapp.ui.animeHome.AnimeHomeViewModel
import com.example.animeapp.utils.Resource
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
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
class AnimeHomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val animeHomeRepository: AnimeHomeRepository = mockk()
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository = mockk()
    private lateinit var viewModel: AnimeHomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        coEvery {
            animeHomeRepository.getAnimeSchedules(AnimeSchedulesSearchQueryState())
        } returns Resource.Loading()

        viewModel = AnimeHomeViewModel(animeHomeRepository, animeEpisodeDetailRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Initial state and GetAnimeSchedules dispatch`() = runTest {
        val viewModel = AnimeHomeViewModel(animeHomeRepository, animeEpisodeDetailRepository)

        val initialState = HomeState()
        assertTrue(viewModel.state.value.animeSchedules is Resource.Loading)
        assertEquals(initialState.queryState, viewModel.state.value.queryState)
        assertEquals(initialState.continueWatchingEpisode, viewModel.state.value.continueWatchingEpisode)
        assertEquals(initialState.isRefreshing, viewModel.state.value.isRefreshing)
        assertEquals(initialState.isShowPopup, viewModel.state.value.isShowPopup)
        assertEquals(initialState.isMinimized, viewModel.state.value.isMinimized)
    }

    @Test
    fun `GetAnimeSchedules success`() = runTest {
        val mockResponse = Resource.Success(
            AnimeSchedulesResponse(
                pagination = defaultCompletePagination,
                data = listOf(animeDetailPlaceholder)
            )
        )
        coEvery { animeHomeRepository.getAnimeSchedules(any()) } returns mockResponse

        viewModel.dispatch(HomeAction.GetAnimeSchedules)

        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(mockResponse, viewModel.state.value.animeSchedules)
    }

    @Test
    fun `GetAnimeSchedules failure`() = runTest {
        val errorMessage = "API Error"
        val mockResponse = Resource.Error<AnimeSchedulesResponse>(errorMessage)
        coEvery { animeHomeRepository.getAnimeSchedules(any()) } returns mockResponse

        assertFalse(viewModel.state.value.isRefreshing)
        assertNull(viewModel.state.value.animeSchedules.data)

        viewModel.dispatch(HomeAction.GetAnimeSchedules)

        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(mockResponse, viewModel.state.value.animeSchedules)
    }

    @Test
    fun `ApplyFilters dispatch and state update`() = runTest {
        val initialQueryState = viewModel.state.value.queryState
        val newQueryState = initialQueryState.copy(filter = "monday")

        coEvery { animeHomeRepository.getAnimeSchedules(newQueryState) } returns Resource.Loading()

        viewModel.dispatch(HomeAction.ApplyFilters(newQueryState))

        assertEquals(newQueryState, viewModel.state.value.queryState)

        coVerify(exactly = 1) { animeHomeRepository.getAnimeSchedules(newQueryState) }
    }

    @Test
    fun `FetchContinueWatchingEpisode no cached episode`() = runTest {
        coEvery { animeEpisodeDetailRepository.getCachedLatestWatchedEpisodeDetailComplement() } returns null

        viewModel.dispatch(HomeAction.FetchContinueWatchingEpisode)

        assertNull(viewModel.state.value.continueWatchingEpisode)
        assertFalse(viewModel.state.value.isShowPopup)
        assertFalse(viewModel.state.value.isMinimized)
    }

    @Test
    fun `FetchContinueWatchingEpisode cached episode exists`() = runTest {
        val mockEpisode = episodeDetailComplementPlaceholder
        coEvery { animeEpisodeDetailRepository.getCachedLatestWatchedEpisodeDetailComplement() } returns mockEpisode

        viewModel.dispatch(HomeAction.FetchContinueWatchingEpisode)

        assertEquals(mockEpisode, viewModel.state.value.continueWatchingEpisode)
        assertTrue(viewModel.state.value.isShowPopup)
        assertFalse(viewModel.state.value.isMinimized)
    }

    @Test
    fun `SetMinimized true`() = runTest {
        viewModel.dispatch(HomeAction.SetMinimized(true))
        assertEquals(true, viewModel.state.value.isMinimized)
    }

    @Test
    fun `SetMinimized false`() = runTest {
        viewModel.dispatch(HomeAction.SetMinimized(false))
        assertEquals(false, viewModel.state.value.isMinimized)
    }

    @Test
    fun `SetShowPopup true`() = runTest {
        viewModel.dispatch(HomeAction.SetShowPopup(true))
        assertEquals(true, viewModel.state.value.isShowPopup)
    }

    @Test
    fun `SetShowPopup false`() = runTest {
        viewModel.dispatch(HomeAction.SetShowPopup(false))
        assertEquals(false, viewModel.state.value.isShowPopup)
    }

    @Test
    fun `Edge case  ApplyFilters with default query`() = runTest {
        val defaultQueryState = AnimeSchedulesSearchQueryState()

        clearMocks(animeHomeRepository)

        coEvery { animeHomeRepository.getAnimeSchedules(defaultQueryState) } returns Resource.Loading()

        viewModel.dispatch(HomeAction.ApplyFilters(defaultQueryState))

        assertEquals(defaultQueryState, viewModel.state.value.queryState)
        coVerify(exactly = 1) { animeHomeRepository.getAnimeSchedules(defaultQueryState) }
    }

    @Test
    fun `Edge case  FetchContinueWatchingEpisode repository empty`() = runTest {
        coEvery { animeEpisodeDetailRepository.getCachedLatestWatchedEpisodeDetailComplement() } returns null

        viewModel.dispatch(HomeAction.FetchContinueWatchingEpisode)

        assertNull(viewModel.state.value.continueWatchingEpisode)
        assertFalse(viewModel.state.value.isShowPopup)
        assertFalse(viewModel.state.value.isMinimized)
    }

    @Test
    fun `Edge case  StateFlow multiple updates`() = runTest {
        val mockResponseSuccess = Resource.Success(AnimeSchedulesResponse(
            pagination = defaultCompletePagination,
            data = listOf(animeDetailPlaceholder)
        ))

        clearMocks(animeHomeRepository)

        coEvery { animeHomeRepository.getAnimeSchedules(any()) } returns mockResponseSuccess

        viewModel.dispatch(HomeAction.ApplyFilters(AnimeSchedulesSearchQueryState(filter = "monday")))
        viewModel.dispatch(HomeAction.ApplyFilters(AnimeSchedulesSearchQueryState(filter = "tuesday")))
        viewModel.dispatch(HomeAction.ApplyFilters(AnimeSchedulesSearchQueryState(filter = "wednesday")))

        advanceTimeBy(1)

        assertEquals(
            AnimeSchedulesSearchQueryState(filter = "wednesday"),
            viewModel.state.value.queryState
        )

        coVerify(exactly = 3) { animeHomeRepository.getAnimeSchedules(any()) }
    }
}