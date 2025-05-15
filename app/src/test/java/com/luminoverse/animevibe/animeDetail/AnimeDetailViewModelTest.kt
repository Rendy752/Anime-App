package com.luminoverse.animevibe.animeDetail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.luminoverse.animevibe.models.*
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.ui.animeDetail.AnimeDetailViewModel
import com.luminoverse.animevibe.ui.animeDetail.DetailAction
import com.luminoverse.animevibe.utils.AnimeTitleFinder
import com.luminoverse.animevibe.utils.ComplementUtils
import com.luminoverse.animevibe.utils.FilterUtils
import com.luminoverse.animevibe.utils.Resource
import com.luminoverse.animevibe.utils.StreamingUtils
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
class AnimeDetailViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AnimeDetailViewModel
    private lateinit var animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        animeEpisodeDetailRepository = mockk()

        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0

        mockkObject(AnimeTitleFinder)
        mockkObject(StreamingUtils)
        mockkObject(FilterUtils)
        mockkObject(ComplementUtils)

        coEvery { animeEpisodeDetailRepository.getAnimeDetail(1735) } returns Resource.Success(
            AnimeDetailResponse(data = animeDetailPlaceholder)
        )
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns null
        coEvery { animeEpisodeDetailRepository.insertCachedAnimeDetailComplement(any()) } just Runs
        coEvery { animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(any()) } just Runs
        coEvery { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) } returns Resource.Success(
            AnimeAniwatchSearchResponse(animes = listOf(animeAniwatchPlaceholder))
        )
        coEvery { animeEpisodeDetailRepository.getEpisodes("anime-1735") } returns Resource.Success(
            EpisodesResponse(
                totalEpisodes = 1,
                episodes = listOf(episodePlaceholder)
            )
        )
        coEvery {
            AnimeTitleFinder.findClosestMatches<AnimeAniwatch>(
                any(), listOf(animeAniwatchPlaceholder), any(), any()
            )
        } returns listOf(animeAniwatchPlaceholder)
        coEvery { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") } returns Resource.Success(
            episodeServersResponsePlaceholder
        )
        coEvery {
            animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "server1", "sub")
        } returns Resource.Error("Server1 failed")
        coEvery {
            animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "server2", "sub")
        } returns Resource.Error("Server2 failed")
        coEvery {
            animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "vidstreaming", "sub")
        } returns Resource.Success(episodeSourcesResponsePlaceholder)
        coEvery {
            animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "dubserver1", "dub")
        } returns Resource.Error("Dubserver1 failed")
        coEvery {
            animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "dubserver2", "dub")
        } returns Resource.Success(episodeSourcesResponsePlaceholder)
        coEvery { animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(any()) } just Runs
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(any(), any(), 1735)
        } returns animeDetailComplementPlaceholder
        coEvery {
            ComplementUtils.updateAnimeDetailComplementWithEpisodes(any(), any(), any(), any())
        } returns animeDetailComplementPlaceholder
        coEvery {
            ComplementUtils.createEpisodeDetailComplement(any(), any(), any(), any(), any(), any(), any())
        } returns episodeDetailComplementPlaceholder
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkStatic("android.util.Log")
        unmockkObject(AnimeTitleFinder)
        unmockkObject(StreamingUtils)
        unmockkObject(FilterUtils)
        unmockkObject(ComplementUtils)
    }

    @Test
    fun `loadAnimeDetail should update state with anime details and trigger loadEpisodes`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue("Anime detail should be success", state.animeDetail is Resource.Success)
        assertTrue("Anime detail complement should be success", state.animeDetailComplement is Resource.Success)
        assertEquals("lorem-ipsum-123?ep=123", state.defaultEpisodeId)
        assertTrue("Episode detail complements should contain key", state.episodeDetailComplements.containsKey("lorem-ipsum-123?ep=123"))
        assertTrue("Episode detail complement should be success", state.episodeDetailComplements["lorem-ipsum-123?ep=123"] is Resource.Success)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(animeId) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodes("anime-1735") }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "vidstreaming", "sub") }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "server1", "sub") }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "server2", "sub") }
    }

    @Test
    fun `loadAnimeDetail should prioritize last server in sub category`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue("Anime detail should be success", state.animeDetail is Resource.Success)
        assertTrue("Anime detail complement should be success", state.animeDetailComplement is Resource.Success)
        assertEquals("lorem-ipsum-123?ep=123", state.defaultEpisodeId)
        assertTrue("Episode detail complements should contain key", state.episodeDetailComplements.containsKey("lorem-ipsum-123?ep=123"))
        assertTrue("Episode detail complement should be success", state.episodeDetailComplements["lorem-ipsum-123?ep=123"] is Resource.Success)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "vidstreaming", "sub") }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "server1", "sub") }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "server2", "sub") }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "dubserver1", "dub") }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "dubserver2", "dub") }
    }

    @Test
    fun `loadAnimeDetail should handle error and not trigger loadEpisodes`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735
        coEvery { animeEpisodeDetailRepository.getAnimeDetail(animeId) } returns Resource.Error("Network error")

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue("Anime detail should be error", state.animeDetail is Resource.Error)
        assertEquals("Network error", (state.animeDetail as Resource.Error).message)
        assertTrue("Anime detail complement should be loading", state.animeDetailComplement is Resource.Loading)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(animeId) }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
    }

    @Test
    fun `loadAnimeDetail should handle null episode sources query`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735
        coEvery {
            animeEpisodeDetailRepository.getEpisodeSources(any(), any(), any())
        } returns Resource.Error("All servers failed")

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue("Anime detail should be success", state.animeDetail is Resource.Success)
        assertTrue("Anime detail complement should be error", state.animeDetailComplement is Resource.Error)
        assertEquals("No valid episode found", (state.animeDetailComplement as Resource.Error).message)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(animeId) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodes("anime-1735") }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(any()) }
    }

    @Test
    fun `loadAnimeDetail should handle mismatched malID in episode sources`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735
        val mismatchedSourcesResponse = mockk<EpisodeSourcesResponse> {
            every { malID } returns 9999
        }
        coEvery {
            animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "vidstreaming", "sub")
        } returns Resource.Success(mismatchedSourcesResponse)

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue("Anime detail should be success", state.animeDetail is Resource.Success)
        assertTrue("Anime detail complement should be error", state.animeDetailComplement is Resource.Error)
        assertEquals("No valid episode found", (state.animeDetailComplement as Resource.Error).message)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(animeId) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodes("anime-1735") }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "vidstreaming", "sub") }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(any()) }
    }

    @Test
    fun `loadAnimeDetail should handle episode sources failure`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735
        coEvery {
            animeEpisodeDetailRepository.getEpisodeSources(any(), any(), any())
        } returns Resource.Error("Server error")

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue("Anime detail should be success", state.animeDetail is Resource.Success)
        assertTrue("Anime detail complement should be error", state.animeDetailComplement is Resource.Error)
        assertEquals("No valid episode found", (state.animeDetailComplement as Resource.Error).message)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(animeId) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodes("anime-1735") }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(any()) }
    }

    @Test
    fun `loadRelationAnimeDetail should update relationAnimeDetails with anime detail`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val relationId = 2
        coEvery { animeEpisodeDetailRepository.getAnimeDetail(relationId) } returns Resource.Success(
            AnimeDetailResponse(data = animeDetailPlaceholder.copy(mal_id = 2, title = "Related Anime"))
        )

        viewModel.onAction(DetailAction.LoadRelationAnimeDetail(relationId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue("Relation anime details should contain key", state.relationAnimeDetails.containsKey(relationId))
        assertTrue("Relation anime detail should be success", state.relationAnimeDetails[relationId] is Resource.Success)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(relationId) }
    }

    @Test
    fun `loadEpisodeDetailComplement should use cached complement if available`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val episodeId = episodeDetailComplementPlaceholder.id
        coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) } returns episodeDetailComplementPlaceholder
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns animeDetailComplementPlaceholder

        viewModel.onAction(DetailAction.LoadAnimeDetail(1735))
        advanceUntilIdle()
        viewModel.onAction(DetailAction.LoadEpisodeDetailComplement(episodeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue("Episode detail complements should contain key", state.episodeDetailComplements.containsKey(episodeId))
        assertTrue("Episode detail complement should be success", state.episodeDetailComplements[episodeId] is Resource.Success)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getEpisodeServers(any()) }
    }

    @Test
    fun `loadEpisodeDetailComplement should fetch servers and sources if no cache`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val episodeId = episodeDetailComplementPlaceholder.id
        val animeId = 1735
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns animeDetailComplementPlaceholder
        coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) } returns null

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()
        viewModel.onAction(DetailAction.LoadEpisodeDetailComplement(episodeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue("Episode detail complements should contain key", state.episodeDetailComplements.containsKey(episodeId))
        assertTrue("Episode detail complement should be error", state.episodeDetailComplements[episodeId] is Resource.Error)
        assertEquals("Episode complement not found", (state.episodeDetailComplements[episodeId] as Resource.Error).message)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getEpisodeServers(episodeId) }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getEpisodeSources(any(), any(), any()) }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(any()) }
    }

    @Test
    fun `loadEpisodes should use cached complement if available`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(animeId) } returns animeDetailComplementPlaceholder
        coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns episodeDetailComplementPlaceholder

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        val detailState = viewModel.detailState.value
        val filterState = viewModel.episodeFilterState.value
        assertTrue("Anime detail complement should be success", detailState.animeDetailComplement is Resource.Success)
        assertTrue("Filtered episodes should not be empty", filterState.filteredEpisodes.isNotEmpty())
        assertEquals("lorem-ipsum-123?ep=123", detailState.defaultEpisodeId)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(animeId) }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(any()) }
    }

    @Test
    fun `loadEpisodes should handle music type anime correctly`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735
        coEvery { animeEpisodeDetailRepository.getAnimeDetail(animeId) } returns Resource.Success(
            AnimeDetailResponse(data = animeDetailPlaceholder.copy(mal_id = 1735, type = "Music"))
        )
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(animeId) } returns null

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue("Anime detail complement should be success", state.animeDetailComplement is Resource.Success)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(animeId) }
        coVerify(exactly = 1) { ComplementUtils.getOrCreateAnimeDetailComplement(any(), any(), animeId) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.insertCachedAnimeDetailComplement(any()) }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
    }

    @Test
    fun `updateEpisodeQueryState should filter episodes based on query`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735
        val query = FilterUtils.EpisodeQueryState(title = "Title of Episode")
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns animeDetailComplementPlaceholder
        coEvery {
            AnimeTitleFinder.searchTitle(
                searchQuery = query.title,
                items = any<List<Episode>>(),
                extractors = any()
            )
        } returns listOf(episodePlaceholder)

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()
        viewModel.onAction(DetailAction.UpdateEpisodeQueryState(query))
        advanceUntilIdle()

        val filterState = viewModel.episodeFilterState.value
        assertEquals(query, filterState.episodeQuery)
        assertTrue("Filtered episodes should not be empty", filterState.filteredEpisodes.isNotEmpty())
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) }
        coVerify(exactly = 1) {
            AnimeTitleFinder.searchTitle(
                searchQuery = query.title,
                items = any<List<Episode>>(),
                extractors = any()
            )
        }
    }

    @Test
    fun `toggleFavorite should update favorite status and cache`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns animeDetailComplementPlaceholder
        coEvery { animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(any()) } just Runs
        coEvery {
            ComplementUtils.toggleAnimeFavorite(any(), any(), 1735, true)
        } returns animeDetailComplementPlaceholder.copy(isFavorite = true)

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()
        viewModel.onAction(DetailAction.ToggleFavorite(true))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue("Anime detail complement should be success", state.animeDetailComplement is Resource.Success)
        assertTrue("isFavorite should be true", state.animeDetailComplement.data?.isFavorite == true)
        coVerify(exactly = 2) { animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(any()) }
    }
}