package com.example.animeapp.animeDetail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.animeapp.models.*
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.ui.animeDetail.AnimeDetailViewModel
import com.example.animeapp.ui.animeDetail.DetailAction
import com.example.animeapp.utils.AnimeTitleFinder
import com.example.animeapp.utils.ComplementUtils
import com.example.animeapp.utils.FilterUtils
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
import retrofit2.Response
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
            AnimeDetailResponse(data = animeDetailPlaceholder.copy(mal_id = 1735))
        )
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns null
        coEvery { animeEpisodeDetailRepository.insertCachedAnimeDetailComplement(any()) } just Runs
        coEvery { animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(any()) } just Runs
        coEvery { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) } returns Response.success(
            AnimeAniwatchSearchResponse(animes = listOf(animeAniwatchPlaceholder.copy(id = "anime-1735")))
        )
        coEvery { animeEpisodeDetailRepository.getEpisodes("anime-1735") } returns Resource.Success(
            EpisodesResponse(
                totalEpisodes = 1,
                episodes = listOf(episodePlaceholder.copy(episodeId = "lorem-ipsum-123?ep=123"))
            )
        )
        coEvery {
            AnimeTitleFinder.findClosestMatches<AnimeAniwatch>(
                any(), listOf(animeAniwatchPlaceholder), any(), any()
            )
        } returns listOf(animeAniwatchPlaceholder.copy(id = "anime-1735"))
        coEvery {
            StreamingUtils.getEpisodeQuery(any(), any())
        } returns episodeSourcesQueryPlaceholder
        coEvery { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") } returns Resource.Success(
            episodeServersResponsePlaceholder
        )
        coEvery {
            animeEpisodeDetailRepository.getEpisodeSources(
                "lorem-ipsum-123?ep=123",
                "vidstreaming",
                "sub"
            )
        } returns Response.success(episodeSourcesResponsePlaceholder.copy(malID = 1735))
        coEvery { animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(any()) } just Runs
        coEvery {
            ComplementUtils.getOrCreateAnimeDetailComplement(any(), any(), 1735)
        } returns animeDetailComplementPlaceholder.copy(episodes = emptyList())
        coEvery {
            ComplementUtils.updateAnimeDetailComplementWithEpisodes(
                any(), any(), any(), any()
            )
        } returns animeDetailComplementPlaceholder.copy(episodes = listOf(episodePlaceholder))
        coEvery {
            ComplementUtils.createEpisodeDetailComplement(
                any(), any(), any(), any(), any(), any(), any()
            )
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
    fun `loadAnimeDetail should update state with anime details and trigger loadEpisodes`() =
        runTest {
            viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
            val animeId = 1735

            viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
            advanceUntilIdle()

            val state = viewModel.detailState.value
            assertTrue("Anime detail should be success", state.animeDetail is Resource.Success)
            if (state.animeDetailComplement !is Resource.Success) {
                println("Anime detail complement state: ${state.animeDetailComplement}")
            }
            assertTrue(
                "Anime detail complement should be success",
                state.animeDetailComplement is Resource.Success
            )
            assertEquals("lorem-ipsum-123?ep=123", state.defaultEpisodeId)
            coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(animeId) }
            coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
            coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodes("anime-1735") }
            coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") }
            coVerify(exactly = 1) {
                animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(any())
            }
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
        assertTrue(
            "Anime detail complement should be loading",
            state.animeDetailComplement is Resource.Loading
        )
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(animeId) }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
    }

    @Test
    fun `loadRelationAnimeDetail should update relationAnimeDetails with anime detail`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val relationId = 2
        val relationAnimeDetail = animeDetailPlaceholder.copy(mal_id = 2, title = "Related Anime")
        coEvery { animeEpisodeDetailRepository.getAnimeDetail(relationId) } returns Resource.Success(
            AnimeDetailResponse(data = relationAnimeDetail)
        )

        viewModel.onAction(DetailAction.LoadRelationAnimeDetail(relationId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue(
            "Relation anime details should contain key",
            state.relationAnimeDetails.containsKey(relationId)
        )
        assertTrue(
            "Relation anime detail should be success",
            state.relationAnimeDetails[relationId] is Resource.Success
        )
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(relationId) }
    }

    @Test
    fun `loadEpisodeDetailComplement should use cached complement if available`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val episodeId = episodeDetailComplementPlaceholder.id
        coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) } returns episodeDetailComplementPlaceholder
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns animeDetailComplementPlaceholder.copy(
            episodes = listOf(episodePlaceholder.copy(episodeId = episodeId))
        )

        viewModel.onAction(DetailAction.LoadAnimeDetail(1735))
        advanceUntilIdle()
        viewModel.onAction(DetailAction.LoadEpisodeDetailComplement(episodeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue(
            "Episode detail complements should contain key",
            state.episodeDetailComplements.containsKey(episodeId)
        )
        if (state.episodeDetailComplements[episodeId] !is Resource.Success) {
            println("Episode detail complement state: ${state.episodeDetailComplements[episodeId]}")
        }
        assertTrue(
            "Episode detail complement should be success",
            state.episodeDetailComplements[episodeId] is Resource.Success
        )
        coVerify(exactly = 1) {
            animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(
                episodeId
            )
        }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getEpisodeServers(any()) }
    }

    @Test
    fun `loadEpisodeDetailComplement should fetch servers and sources if no cache`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val episodeId = episodeDetailComplementPlaceholder.id
        val animeId = 1735
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns animeDetailComplementPlaceholder.copy(
            episodes = listOf(episodePlaceholder.copy(episodeId = episodeId))
        )

        coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) } returns null

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()
        viewModel.onAction(DetailAction.LoadEpisodeDetailComplement(episodeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue(
            "Episode detail complements should contain key",
            state.episodeDetailComplements.containsKey(episodeId)
        )
        if (state.episodeDetailComplements[episodeId] !is Resource.Success) {
            println("Episode detail complement state: ${state.episodeDetailComplements[episodeId]}")
        }
        assertTrue(
            "Episode detail complement should be success",
            state.episodeDetailComplements[episodeId] is Resource.Success
        )
        coVerify(exactly = 1) {
            animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(
                episodeId
            )
        }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeServers(episodeId) }
        coVerify(exactly = 1) {
            animeEpisodeDetailRepository.getEpisodeSources(
                episodeId,
                "vidstreaming",
                "sub"
            )
        }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(any()) }
    }

    @Test
    fun `loadEpisodes should use cached complement if available`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(animeId) } returns animeDetailComplementPlaceholder.copy(
            episodes = listOf(episodePlaceholder.copy(episodeId = "lorem-ipsum-123?ep=123"))
        )
        coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns episodeDetailComplementPlaceholder

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        val detailState = viewModel.detailState.value
        val filterState = viewModel.episodeFilterState.value
        if (detailState.animeDetailComplement !is Resource.Success) {
            println("Anime detail complement state: ${detailState.animeDetailComplement}")
        }
        assertTrue(
            "Anime detail complement should be success",
            detailState.animeDetailComplement is Resource.Success
        )
        assertTrue(
            "Filtered episodes should not be empty",
            filterState.filteredEpisodes.isNotEmpty()
        )
        assertEquals("lorem-ipsum-123?ep=123", detailState.defaultEpisodeId)
        coVerify(exactly = 1) {
            animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(
                animeId
            )
        }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(any()) }
    }

    @Test
    fun `loadEpisodes should handle music type anime correctly`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735
        val musicAnimeDetail = animeDetailPlaceholder.copy(mal_id = 1735, type = "Music")
        coEvery { animeEpisodeDetailRepository.getAnimeDetail(animeId) } returns Resource.Success(
            AnimeDetailResponse(data = musicAnimeDetail)
        )
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(animeId) } returns null

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        if (state.animeDetailComplement !is Resource.Success) {
            println("Anime detail complement state: ${state.animeDetailComplement}")
        }
        assertTrue(
            "Anime detail complement should be success",
            state.animeDetailComplement is Resource.Success
        )
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(animeId) }
        coVerify(exactly = 1) {
            ComplementUtils.getOrCreateAnimeDetailComplement(any(), any(), animeId)
        }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.insertCachedAnimeDetailComplement(any()) }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
    }

    @Test
    fun `updateEpisodeQueryState should filter episodes based on query`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735
        val query = FilterUtils.EpisodeQueryState(title = "Title of Episode")
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns animeDetailComplementPlaceholder.copy(
            episodes = listOf(episodePlaceholder)
        )
        coEvery { FilterUtils.filterEpisodes(any(), query, any()) } returns listOf(
            episodePlaceholder
        )

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()
        viewModel.onAction(DetailAction.UpdateEpisodeQueryState(query))
        advanceUntilIdle()

        val filterState = viewModel.episodeFilterState.value
        assertEquals(query, filterState.episodeQuery)
        assertTrue(
            "Filtered episodes should not be empty",
            filterState.filteredEpisodes.isNotEmpty()
        )
        coVerify(exactly = 1) {
            animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(
                1735
            )
        }
        coVerify(exactly = 1) { FilterUtils.filterEpisodes(any(), query, any()) }
    }

    @Test
    fun `toggleFavorite should update favorite status and cache`() = runTest {
        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
        val animeId = 1735
        val updatedComplement = animeDetailComplementPlaceholder.copy(isFavorite = true)
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns animeDetailComplementPlaceholder
        coEvery { animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(any()) } just Runs
        coEvery {
            ComplementUtils.toggleAnimeFavorite(any(), any(), 1735, true)
        } returns updatedComplement

        viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
        advanceUntilIdle()
        viewModel.onAction(DetailAction.ToggleFavorite(true))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue(
            "Anime detail complement should be success",
            state.animeDetailComplement is Resource.Success
        )
        assertTrue(
            "isFavorite should be true",
            state.animeDetailComplement.data?.isFavorite == true
        )
        coVerify(exactly = 2) { animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(any()) }
    }
}