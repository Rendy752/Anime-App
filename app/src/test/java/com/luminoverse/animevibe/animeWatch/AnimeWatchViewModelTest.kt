package com.luminoverse.animevibe.animeWatch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.ui.animeWatch.AnimeWatchViewModel
import com.luminoverse.animevibe.ui.animeWatch.WatchAction
import com.luminoverse.animevibe.utils.ComplementUtils
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.media.StreamingUtils
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
        } returns Resource.Success(mockEpisodeSources)
        coEvery { animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(any()) } just Runs
        coEvery { animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(any()) } just Runs
        coEvery { animeEpisodeDetailRepository.updateEpisodeDetailComplement(any()) } just Runs

        mockkObject(StreamingUtils)
        coEvery {
            StreamingUtils.getEpisodeSources(
                any(),
                any(),
                any()
            )
        } returns Pair(
            Resource.Success(mockEpisodeSources),
            mockEpisodeDetailComplement.sourcesQuery
        )
        coEvery { StreamingUtils.markServerFailed(any(), any()) } just Runs

        viewModel = AnimeWatchViewModel(animeEpisodeDetailRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        StreamingUtils.failedServers.clear()
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
            assertTrue(watchState.episodeDetailComplement is Resource.Success)
            assertEquals(
                favoriteComplement,
                (watchState.episodeDetailComplement as Resource.Success).data
            )
            assertEquals(query, watchState.episodeSourcesQuery)
            assertTrue(watchState.isFavorite)
            assertEquals(false, watchState.isRefreshing)
            coVerify(exactly = 1) { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") }
            coVerify(exactly = 0) { animeEpisodeDetailRepository.getEpisodeServers(any()) }
            coVerify(exactly = 0) {
                animeEpisodeDetailRepository.getEpisodeSources(
                    any(),
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `HandleSelectedEpisodeServer with server error should restore default and set error`() =
        runTest {
            val query = episodeSourcesQueryPlaceholder.copy(id = "lorem-ipsum-123?ep=123")
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns mockEpisodeDetailComplement
            coEvery { animeEpisodeDetailRepository.getCachedDefaultEpisodeDetailComplementByMalId(1) } returns mockEpisodeDetailComplement
            coEvery { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") } returns Resource.Error(
                "Server error"
            )
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
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns null
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
            coVerify(exactly = 0) {
                animeEpisodeDetailRepository.getEpisodeSources(
                    any(),
                    any(),
                    any()
                )
            }
            unmockkObject(ComplementUtils)
        }

    @Test
    fun `HandleSelectedEpisodeServer with multiple server failures should retry and fail after max attempts`() =
        runTest {
            val query = episodeSourcesQueryPlaceholder.copy(
                id = "lorem-ipsum-123?ep=123",
                server = "server1",
                category = "sub"
            )
            val alternativeQuery = episodeSourcesQueryPlaceholder.copy(
                id = "lorem-ipsum-123?ep=123",
                server = "server2",
                category = "sub"
            )

            val defaultQueries = listOf(query, alternativeQuery)
            val failedServers = mutableMapOf<String, Long>()
            every { StreamingUtils.failedServers } returns failedServers
            coEvery { StreamingUtils.isServerRecentlyFailed(any(), any()) } returns false
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns mockEpisodeDetailComplement
            coEvery { animeEpisodeDetailRepository.getCachedDefaultEpisodeDetailComplementByMalId(1) } returns mockEpisodeDetailComplement
            coEvery { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") } returns Resource.Success(
                mockEpisodeServers
            )
            coEvery {
                StreamingUtils.getEpisodeSources(
                    any(),
                    any(),
                    query
                )
            } returns Pair(Resource.Error("Source error"), alternativeQuery)
            coEvery {
                StreamingUtils.getEpisodeSources(
                    any(),
                    any(),
                    alternativeQuery
                )
            } returns Pair(Resource.Error("Source error"), null)
            mockkObject(ComplementUtils)
            coEvery {
                ComplementUtils.getOrCreateAnimeDetailComplement(
                    repository = animeEpisodeDetailRepository,
                    malId = 1
                )
            } returns mockAnimeDetailComplement
            coEvery { StreamingUtils.getDefaultEpisodeQueries(any(), any()) } returns defaultQueries
            coEvery { StreamingUtils.markServerFailed(any(), any()) } answers {
                failedServers["${args[0]}-${args[1]}"] = System.currentTimeMillis()
            }

            viewModel.onAction(
                WatchAction.SetInitialState(
                    malId = 1,
                    episodeId = "lorem-ipsum-123?ep=123"
                )
            )
            advanceUntilIdle()
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns null
            viewModel.onAction(WatchAction.HandleSelectedEpisodeServer(query, isFirstInit = false))
            advanceUntilIdle()

            val watchState = viewModel.watchState.value
            assertTrue(watchState.episodeDetailComplement is Resource.Error)
            assertEquals(
                "Failed to fetch episode sources after 2 attempts",
                (watchState.episodeDetailComplement as Resource.Error).message
            )
            assertEquals(mockEpisodeDetailComplement.sourcesQuery, watchState.episodeSourcesQuery)
            assertEquals(false, watchState.isRefreshing)
            coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") }
            coVerify(exactly = 1) { StreamingUtils.getEpisodeSources(any(), any(), query) }
            coVerify(exactly = 1) {
                StreamingUtils.getEpisodeSources(
                    any(),
                    any(),
                    alternativeQuery
                )
            }
            coVerify(exactly = 2) { StreamingUtils.markServerFailed(any(), any()) }
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
        assertTrue(playerUiState.isFullscreen)
    }
}