package com.luminoverse.animevibe.animeHome

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.luminoverse.animevibe.models.AnimeSchedulesSearchQueryState
import com.luminoverse.animevibe.models.ListAnimeDetailResponse
import com.luminoverse.animevibe.models.animeDetailPlaceholder
import com.luminoverse.animevibe.models.defaultCompletePagination
import com.luminoverse.animevibe.models.episodeDetailComplementPlaceholder
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.repository.AnimeHomeRepository
import com.luminoverse.animevibe.ui.animeHome.HomeAction
import com.luminoverse.animevibe.ui.animeHome.HomeState
import com.luminoverse.animevibe.ui.animeHome.AnimeHomeViewModel
import com.luminoverse.animevibe.ui.animeHome.CarouselState
import com.luminoverse.animevibe.utils.Resource
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

        coEvery {
            animeHomeRepository.getTop10Anime()
        } returns Resource.Loading()

        viewModel = AnimeHomeViewModel(animeHomeRepository, animeEpisodeDetailRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Initial state and GetAnimeSchedules onAction`() = runTest {
        val viewModel = AnimeHomeViewModel(animeHomeRepository, animeEpisodeDetailRepository)

        val initialState = HomeState()
        val carouselState = CarouselState()
        assertTrue(viewModel.homeState.value.animeSchedules is Resource.Loading)
        assertTrue(viewModel.homeState.value.top10Anime is Resource.Loading)
        assertEquals(initialState.queryState, viewModel.homeState.value.queryState)
        assertEquals(initialState.continueWatchingEpisode, viewModel.homeState.value.continueWatchingEpisode)
        assertEquals(initialState.isRefreshing, viewModel.homeState.value.isRefreshing)
        assertEquals(initialState.isShowPopup, viewModel.homeState.value.isShowPopup)
        assertEquals(initialState.isMinimized, viewModel.homeState.value.isMinimized)
        assertEquals(carouselState.currentCarouselPage, viewModel.carouselState.value.currentCarouselPage)
        assertEquals(carouselState.autoScrollEnabled, viewModel.carouselState.value.autoScrollEnabled)
    }

    @Test
    fun `GetAnimeSchedules success`() = runTest {
        val mockResponse = Resource.Success(
            ListAnimeDetailResponse(
                pagination = defaultCompletePagination,
                data = listOf(animeDetailPlaceholder)
            )
        )
        coEvery { animeHomeRepository.getAnimeSchedules(any()) } returns mockResponse

        viewModel.onAction(HomeAction.GetAnimeSchedules)

        assertFalse(viewModel.homeState.value.isRefreshing)
        assertEquals(mockResponse, viewModel.homeState.value.animeSchedules)
    }

    @Test
    fun `GetAnimeSchedules failure`() = runTest {
        val errorMessage = "API Error"
        val mockResponse = Resource.Error<ListAnimeDetailResponse>(errorMessage)
        coEvery { animeHomeRepository.getAnimeSchedules(any()) } returns mockResponse

        assertFalse(viewModel.homeState.value.isRefreshing)
        assertNull(viewModel.homeState.value.animeSchedules.data)

        viewModel.onAction(HomeAction.GetAnimeSchedules)

        assertFalse(viewModel.homeState.value.isRefreshing)
        assertEquals(mockResponse, viewModel.homeState.value.animeSchedules)
    }

    @Test
    fun `GetTop10Anime success`() = runTest {
        val mockResponse = Resource.Success(
            ListAnimeDetailResponse(
                pagination = defaultCompletePagination,
                data = listOf(animeDetailPlaceholder)
            )
        )
        coEvery { animeHomeRepository.getTop10Anime() } returns mockResponse

        viewModel.onAction(HomeAction.GetTop10Anime)

        assertEquals(mockResponse, viewModel.homeState.value.top10Anime)
    }

    @Test
    fun `GetTop10Anime failure`() = runTest {
        val errorMessage = "API Error"
        val mockResponse = Resource.Error<ListAnimeDetailResponse>(errorMessage)
        coEvery { animeHomeRepository.getTop10Anime() } returns mockResponse

        assertNull(viewModel.homeState.value.top10Anime.data)

        viewModel.onAction(HomeAction.GetTop10Anime)

        assertEquals(mockResponse, viewModel.homeState.value.top10Anime)
    }

    @Test
    fun `ApplyFilters onAction and state update`() = runTest {
        val initialQueryState = viewModel.homeState.value.queryState
        val newQueryState = initialQueryState.copy(filter = "monday")

        coEvery { animeHomeRepository.getAnimeSchedules(newQueryState) } returns Resource.Loading()

        viewModel.onAction(HomeAction.ApplyFilters(newQueryState))

        assertEquals(newQueryState, viewModel.homeState.value.queryState)

        coVerify(exactly = 1) { animeHomeRepository.getAnimeSchedules(newQueryState) }
    }

    @Test
    fun `FetchContinueWatchingEpisode no cached episode`() = runTest {
        coEvery { animeEpisodeDetailRepository.getCachedLatestWatchedEpisodeDetailComplement() } returns null

        viewModel.onAction(HomeAction.FetchContinueWatchingEpisode)

        assertNull(viewModel.homeState.value.continueWatchingEpisode)
        assertFalse(viewModel.homeState.value.isShowPopup)
        assertFalse(viewModel.homeState.value.isMinimized)
    }

    @Test
    fun `FetchContinueWatchingEpisode cached episode exists`() = runTest {
        val mockEpisode = episodeDetailComplementPlaceholder
        coEvery { animeEpisodeDetailRepository.getCachedLatestWatchedEpisodeDetailComplement() } returns mockEpisode

        viewModel.onAction(HomeAction.FetchContinueWatchingEpisode)

        assertEquals(mockEpisode, viewModel.homeState.value.continueWatchingEpisode)
        assertTrue(viewModel.homeState.value.isShowPopup)
        assertFalse(viewModel.homeState.value.isMinimized)
    }

    @Test
    fun `SetMinimized true`() = runTest {
        viewModel.onAction(HomeAction.SetMinimized(true))
        assertEquals(true, viewModel.homeState.value.isMinimized)
    }

    @Test
    fun `SetMinimized false`() = runTest {
        viewModel.onAction(HomeAction.SetMinimized(false))
        assertEquals(false, viewModel.homeState.value.isMinimized)
    }

    @Test
    fun `SetShowPopup true`() = runTest {
        viewModel.onAction(HomeAction.SetShowPopup(true))
        assertEquals(true, viewModel.homeState.value.isShowPopup)
    }

    @Test
    fun `SetShowPopup false`() = runTest {
        viewModel.onAction(HomeAction.SetShowPopup(false))
        assertEquals(false, viewModel.homeState.value.isShowPopup)
    }

    @Test
    fun `Edge case  ApplyFilters with default query`() = runTest {
        val defaultQueryState = AnimeSchedulesSearchQueryState()

        clearMocks(animeHomeRepository)

        coEvery { animeHomeRepository.getAnimeSchedules(defaultQueryState) } returns Resource.Loading()

        viewModel.onAction(HomeAction.ApplyFilters(defaultQueryState))

        assertEquals(defaultQueryState, viewModel.homeState.value.queryState)
        coVerify(exactly = 1) { animeHomeRepository.getAnimeSchedules(defaultQueryState) }
    }

    @Test
    fun `Edge case  FetchContinueWatchingEpisode repository empty`() = runTest {
        coEvery { animeEpisodeDetailRepository.getCachedLatestWatchedEpisodeDetailComplement() } returns null

        viewModel.onAction(HomeAction.FetchContinueWatchingEpisode)

        assertNull(viewModel.homeState.value.continueWatchingEpisode)
        assertFalse(viewModel.homeState.value.isShowPopup)
        assertFalse(viewModel.homeState.value.isMinimized)
    }

    @Test
    fun `Edge case  StateFlow multiple updates`() = runTest {
        val mockResponseSuccess = Resource.Success(ListAnimeDetailResponse(
            pagination = defaultCompletePagination,
            data = listOf(animeDetailPlaceholder)
        ))

        clearMocks(animeHomeRepository)

        coEvery { animeHomeRepository.getAnimeSchedules(any()) } returns mockResponseSuccess

        viewModel.onAction(HomeAction.ApplyFilters(AnimeSchedulesSearchQueryState(filter = "monday")))
        viewModel.onAction(HomeAction.ApplyFilters(AnimeSchedulesSearchQueryState(filter = "tuesday")))
        viewModel.onAction(HomeAction.ApplyFilters(AnimeSchedulesSearchQueryState(filter = "wednesday")))

        advanceTimeBy(1)

        assertEquals(
            AnimeSchedulesSearchQueryState(filter = "wednesday"),
            viewModel.homeState.value.queryState
        )

        coVerify(exactly = 3) { animeHomeRepository.getAnimeSchedules(any()) }
    }
}