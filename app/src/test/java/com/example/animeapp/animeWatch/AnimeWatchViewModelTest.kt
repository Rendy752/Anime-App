package com.example.animeapp.animeWatch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.animeapp.models.*
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.ui.animeWatch.AnimeWatchViewModel
import com.example.animeapp.ui.animeWatch.WatchAction
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
        animeEpisodeDetailRepository = mockk()

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
        unmockkObject(StreamingUtils)
        clearAllMocks()
    }

    @Test
    fun `SetInitialState should update animeDetail, animeDetailComplement, and trigger HandleSelectedEpisodeServer`() =
        runTest {
            viewModel.onAction(
                WatchAction.SetInitialState(
                    malId = 1,
                    episodeId = "lorem-ipsum-123?ep=123"
                )
            )
            advanceUntilIdle()

            val watchState = viewModel.watchState.value
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
            coVerify(exactly = 1) {
                animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(
                    1
                )
            }
            coVerify(atMost = 2) { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
        }

    @Test
    fun `HandleSelectedEpisodeServer with cached data should update episodeDetailComplement and isFavorite`() =
        runTest {
            val query = episodeSourcesQueryPlaceholder.copy(id = "lorem-ipsum-123?ep=123")
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns mockEpisodeDetailComplement.copy(
                isFavorite = true
            )

            viewModel.onAction(WatchAction.HandleSelectedEpisodeServer(query, isFirstInit = true))
            advanceUntilIdle()

            val watchState = viewModel.watchState.value
            assertTrue(watchState.episodeDetailComplement is Resource.Success)
            assertEquals(
                mockEpisodeDetailComplement.copy(isFavorite = true),
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
            coEvery { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") } returns Resource.Error(
                "Server error"
            )
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns null

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
            assertTrue(watchState.episodeDetailComplement is Resource.Error)
            assertEquals(
                "Server error",
                (watchState.episodeDetailComplement as Resource.Error).message
            )
            assertEquals(mockEpisodeDetailComplement.sourcesQuery, watchState.episodeSourcesQuery)
            assertEquals(false, watchState.isRefreshing)
            coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") }
        }

    @Test
    fun `LoadEpisodeDetailComplement with cached data should update episodeDetailComplements`() =
        runTest {
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=124") } returns mockEpisodeDetailComplement.copy(
                id = "lorem-ipsum-123?ep=124"
            )

            viewModel.onAction(WatchAction.LoadEpisodeDetailComplement("lorem-ipsum-123?ep=124"))
            advanceUntilIdle()

            val watchState = viewModel.watchState.value
            assertTrue(watchState.episodeDetailComplements["lorem-ipsum-123?ep=124"] is Resource.Success)
            assertEquals(
                mockEpisodeDetailComplement.copy(id = "lorem-ipsum-123?ep=124"),
                (watchState.episodeDetailComplements["lorem-ipsum-123?ep=124"] as Resource.Success).data
            )
            coVerify(exactly = 1) { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=124") }
        }

    @Test
    fun `LoadEpisodeDetailComplement with remote data should insert and update episodeDetailComplements`() =
        runTest {
            val episodeId = "lorem-ipsum-123?ep=124"
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) } returns null
            coEvery { animeEpisodeDetailRepository.getEpisodeServers(episodeId) } returns Resource.Success(
                mockEpisodeServers
            )
            coEvery {
                animeEpisodeDetailRepository.getEpisodeSources(
                    any(),
                    any(),
                    any()
                )
            } returns Response.success(mockEpisodeSources)
            val updatedComplement = mockAnimeDetailComplement.copy(
                episodes = listOf(
                    episodePlaceholder.copy(
                        episodeId = episodeId,
                        episodeNo = 2,
                        name = "Episode 2",
                        filler = false
                    )
                )
            )
            coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1) } returns updatedComplement

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
            assertTrue(watchState.episodeDetailComplements[episodeId] is Resource.Success)
            val complement =
                (watchState.episodeDetailComplements[episodeId] as Resource.Success).data
            assertEquals(episodeId, complement.id)
            assertEquals(mockAnimeDetail.title, complement.animeTitle)
            assertEquals("Episode 2", complement.episodeTitle)
            coVerify(exactly = 1) {
                animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(
                    any()
                )
            }
        }

    @Test
    fun `SetFavorite should update isFavorite and episodeDetailComplement`() = runTest {
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
        assertTrue(watchState.isFavorite)
        assertTrue(watchState.episodeDetailComplement is Resource.Success)
        assertTrue((watchState.episodeDetailComplement as Resource.Success).data.isFavorite)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.updateEpisodeDetailComplement(any()) }
    }

    @Test
    fun `SetFullscreen should update playerUiState isFullscreen`() = runTest {
        viewModel.onAction(WatchAction.SetFullscreen(isFullscreen = true))
        advanceUntilIdle()

        val playerUiState = viewModel.playerUiState.value
        assertTrue(playerUiState.isFullscreen)
    }
}