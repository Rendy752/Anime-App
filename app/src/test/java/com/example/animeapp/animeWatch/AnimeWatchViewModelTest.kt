package com.example.animeapp.animeWatch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.animeapp.models.*
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.ui.animeWatch.AnimeWatchViewModel
import com.example.animeapp.ui.animeWatch.WatchAction
import com.example.animeapp.utils.ComplementUtils
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.StreamingUtils
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import retrofit2.Response

@ExperimentalCoroutinesApi
class AnimeWatchViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AnimeWatchViewModel
    private lateinit var animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
    private lateinit var testDispatcher: TestDispatcher

    private val mockAnimeDetail = animeDetailPlaceholder.copy(
        mal_id = 1,
        title = "Test Anime",
        images = Images(
            jpg = imageUrlPlaceholder.copy(
                image_url = "",
                small_image_url = "",
                large_image_url = ""
            ),
            webp = imageUrlPlaceholder.copy(
                image_url = "",
                small_image_url = "",
                large_image_url = "test_image_url"
            )
        )
    )
    private val mockAnimeDetailComplement = animeDetailComplementPlaceholder.copy(
        id = "aniwatch1",
        malId = 1,
        lastEpisodeWatchedId = "lorem-ipsum-123?ep=123",
        episodes = listOf(
            episodePlaceholder.copy(
                episodeId = "lorem-ipsum-123?ep=123",
                episodeNo = 1,
                name = "Episode 1",
                filler = false
            ),
            episodePlaceholder.copy(
                episodeId = "lorem-ipsum-123?ep=124",
                episodeNo = 2,
                name = "Episode 2",
                filler = false
            )
        )
    )
    private val mockEpisodeDetailComplement = episodeDetailComplementPlaceholder.copy(
        id = "lorem-ipsum-123?ep=123",
        malId = 1,
        aniwatchId = "aniwatch1",
        animeTitle = "Test Anime",
        episodeTitle = "Episode 1",
        imageUrl = "test_image_url",
        number = 1,
        isFiller = false,
        servers = episodeServersResponsePlaceholder.copy(episodeId = "lorem-ipsum-123?ep=123"),
        sources = episodeSourcesResponsePlaceholder,
        sourcesQuery = episodeSourcesQueryPlaceholder.copy(id = "lorem-ipsum-123?ep=123"),
        isFavorite = false
    )
    private val mockEpisodeServers =
        episodeServersResponsePlaceholder.copy(episodeId = "lorem-ipsum-123?ep=123")
    private val mockEpisodeSources = episodeSourcesResponsePlaceholder

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        animeEpisodeDetailRepository = mockk(relaxed = true)

        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailById(1) } returns mockAnimeDetail
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1) } returns mockAnimeDetailComplement
        coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns mockEpisodeDetailComplement
        coEvery { animeEpisodeDetailRepository.getCachedDefaultEpisodeDetailComplementByMalId(1) } returns mockEpisodeDetailComplement
        coEvery { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") } returns Resource.Success(
            mockEpisodeServers
        )
        coEvery {
            animeEpisodeDetailRepository.getEpisodeSources(
                any(),
                any(),
                any()
            )
        } returns Response.success(mockEpisodeSources)
        coEvery { animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(any()) } just Runs
        coEvery { animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(any()) } just Runs
        coEvery { animeEpisodeDetailRepository.updateEpisodeDetailComplement(any()) } just Runs

        mockkObject(StreamingUtils)
        coEvery { StreamingUtils.getEpisodeSources(any(), any(), any()) } returns Resource.Success(
            mockEpisodeSources
        )

        viewModel = AnimeWatchViewModel(animeEpisodeDetailRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `SetInitialState should update animeDetail, animeDetailComplement, and trigger HandleSelectedEpisodeServer`() =
        runTest {
            mockkObject(ComplementUtils)
            coEvery {
                ComplementUtils.getOrCreateAnimeDetailComplement(
                    repository = animeEpisodeDetailRepository,
                    malId = 1
                )
            } returns mockAnimeDetailComplement
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns mockEpisodeDetailComplement

            viewModel.onAction(
                WatchAction.SetInitialState(
                    malId = 1,
                    episodeId = "lorem-ipsum-123?ep=123"
                )
            )
            advanceUntilIdle()

            val watchState = viewModel.watchState.value
            println("SetInitialState watchState: $watchState")
            assertEquals(mockAnimeDetail, watchState.animeDetail)
            assertEquals(mockAnimeDetailComplement, watchState.animeDetailComplement)
            assertTrue(watchState.episodeDetailComplement is Resource.Success)
            assertEquals(
                mockEpisodeDetailComplement,
                (watchState.episodeDetailComplement as Resource.Success).data
            )
            assertEquals(
                episodeSourcesQueryPlaceholder.copy(id = "lorem-ipsum-123?ep=123"),
                watchState.episodeSourcesQuery
            )
            coVerify(exactly = 1) { animeEpisodeDetailRepository.getCachedAnimeDetailById(1) }
            coVerify(exactly = 2) { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
            unmockkObject(ComplementUtils)
        }

    @Test
    fun `HandleSelectedEpisodeServer with cached data should update episodeDetailComplement and isFavorite`() =
        runTest {
            val query = episodeSourcesQueryPlaceholder.copy(id = "lorem-ipsum-123?ep=123")
            val favoriteComplement = mockEpisodeDetailComplement.copy(isFavorite = true)
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns favoriteComplement

            viewModel.onAction(WatchAction.HandleSelectedEpisodeServer(query, isFirstInit = true))
            advanceUntilIdle()

            val watchState = viewModel.watchState.value
            println("HandleSelectedEpisodeServer cached watchState: $watchState")
            assertTrue(watchState.episodeDetailComplement is Resource.Success)
            assertEquals(
                favoriteComplement,
                (watchState.episodeDetailComplement as Resource.Success).data
            )
            assertEquals(query, watchState.episodeSourcesQuery)
            assertTrue(watchState.isFavorite)
            assertEquals(false, watchState.isRefreshing)
            coVerify(exactly = 1) { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
        }

    @Test
    fun `HandleSelectedEpisodeServer with server error should restore default and set error`() =
        runTest {
            val query = episodeSourcesQueryPlaceholder.copy(id = "lorem-ipsum-123?ep=123")
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns mockEpisodeDetailComplement andThen mockEpisodeDetailComplement andThen null
            coEvery { animeEpisodeDetailRepository.getCachedDefaultEpisodeDetailComplementByMalId(1) } returns mockEpisodeDetailComplement
            coEvery { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") } returns Resource.Error("Server error")
            mockkObject(ComplementUtils)
            coEvery {
                ComplementUtils.getOrCreateAnimeDetailComplement(
                    repository = animeEpisodeDetailRepository,
                    malId = 1
                )
            } returns mockAnimeDetailComplement

            viewModel.onAction(
                WatchAction.SetInitialState(
                    malId = 1,
                    episodeId = "lorem-ipsum-123?ep=123"
                )
            )
            advanceUntilIdle()
            viewModel.onAction(WatchAction.HandleSelectedEpisodeServer(query, isFirstInit = false))
            advanceUntilIdle()

            val watchState = viewModel.watchState.value
            println("HandleSelectedEpisodeServer error watchState: $watchState")
            println("mockEpisodeDetailComplement.sourcesQuery: ${mockEpisodeDetailComplement.sourcesQuery}")
            println("query: $query")
            assertTrue(watchState.episodeDetailComplement is Resource.Error)
            assertEquals(
                "Server error",
                (watchState.episodeDetailComplement as Resource.Error).message
            )
            assertEquals(mockEpisodeDetailComplement.sourcesQuery, watchState.episodeSourcesQuery)
            assertEquals(false, watchState.isRefreshing)
            coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") }
            unmockkObject(ComplementUtils)
        }

    @Test
    fun `LoadEpisodeDetailComplement with cached data should update episodeDetailComplements`() =
        runTest {
            val episodeId = "lorem-ipsum-123?ep=124"
            val cachedComplement = mockEpisodeDetailComplement.copy(id = episodeId)
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) } returns cachedComplement

            viewModel.onAction(WatchAction.LoadEpisodeDetailComplement(episodeId))
            advanceUntilIdle()

            val watchState = viewModel.watchState.value
            println("LoadEpisodeDetailComplement cached watchState: $watchState")
            assertTrue(watchState.episodeDetailComplements[episodeId] is Resource.Success)
            assertEquals(
                cachedComplement,
                (watchState.episodeDetailComplements[episodeId] as Resource.Success).data
            )
            coVerify(exactly = 1) {
                animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(
                    episodeId
                )
            }
        }

    @Test
    fun `LoadEpisodeDetailComplement with no cached data should set error in episodeDetailComplements`() =
        runTest {
            val episodeId = "lorem-ipsum-123?ep=124"
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) } returns null

            viewModel.onAction(
                WatchAction.SetInitialState(
                    malId = 1,
                    episodeId = "lorem-ipsum-123?ep=123"
                )
            )
            advanceUntilIdle()
            viewModel.onAction(WatchAction.LoadEpisodeDetailComplement(episodeId))
            advanceUntilIdle()

            val watchState = viewModel.watchState.value
            println("LoadEpisodeDetailComplement no cached watchState: $watchState")
            assertTrue(watchState.episodeDetailComplements[episodeId] is Resource.Error)
            assertEquals(
                "Episode detail complement not found",
                (watchState.episodeDetailComplements[episodeId] as Resource.Error).message
            )
            coVerify(exactly = 1) {
                animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId)
            }
            coVerify(exactly = 0) {
                animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(any())
            }
            coVerify(exactly = 0) {
                animeEpisodeDetailRepository.getEpisodeServers(episodeId)
            }
            coVerify(exactly = 0) {
                animeEpisodeDetailRepository.getEpisodeSources(any(), any(), any())
            }
        }

    @Test
    fun `SetFavorite should update isFavorite and episodeDetailComplement`() = runTest {
        val updatedComplement = mockEpisodeDetailComplement.copy(isFavorite = true)
        mockkObject(ComplementUtils)
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(
                repository = animeEpisodeDetailRepository,
                malId = 1
            )
        } returns mockAnimeDetailComplement
        coEvery {
            ComplementUtils.toggleEpisodeFavorite(
                repository = animeEpisodeDetailRepository,
                episodeId = "lorem-ipsum-123?ep=123",
                isFavorite = true
            )
        } coAnswers {
            animeEpisodeDetailRepository.updateEpisodeDetailComplement(updatedComplement)
            updatedComplement
        }

        viewModel.onAction(
            WatchAction.SetInitialState(
                malId = 1,
                episodeId = "lorem-ipsum-123?ep=123"
            )
        )
        advanceUntilIdle()
        viewModel.onAction(WatchAction.SetFavorite(isFavorite = true))
        advanceUntilIdle()

        val watchState = viewModel.watchState.value
        println("SetFavorite watchState: $watchState")
        assertTrue(watchState.isFavorite)
        assertTrue(watchState.episodeDetailComplement is Resource.Success)
        assertTrue((watchState.episodeDetailComplement as Resource.Success).data.isFavorite)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.updateEpisodeDetailComplement(any()) }
        unmockkObject(ComplementUtils)
    }

    @Test
    fun `SetFullscreen should update playerUiState isFullscreen`() = runTest {
        viewModel.onAction(WatchAction.SetFullscreen(isFullscreen = true))
        advanceUntilIdle()

        val playerUiState = viewModel.playerUiState.value
        println("SetFullscreen playerUiState: $playerUiState")
        assertTrue(playerUiState.isFullscreen)
    }
}