package com.example.animeapp.animeDetail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.animeapp.models.*
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.ui.animeDetail.AnimeDetailViewModel
import com.example.animeapp.ui.animeDetail.DetailAction
import com.example.animeapp.utils.AnimeTitleFinder
import com.example.animeapp.utils.FilterUtils
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.StreamingUtils
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.coroutines.yield
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
    private lateinit var testDispatcher: TestDispatcher

    @MockK
    private lateinit var animeEpisodeDetailRepository: AnimeEpisodeDetailRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0

        mockkObject(AnimeTitleFinder)
        mockkObject(StreamingUtils)
        mockkObject(FilterUtils)

        var getAnimeDetailCallCount = 0
        coEvery { animeEpisodeDetailRepository.getAnimeDetail(1735) } answers {
            getAnimeDetailCallCount++
            if (getAnimeDetailCallCount > 2) throw IllegalStateException("Excessive calls to getAnimeDetail: $getAnimeDetailCallCount")
            println("getAnimeDetail called ($getAnimeDetailCallCount) for id: 1735")
            Resource.Success(AnimeDetailResponse(data = animeDetailPlaceholder))
        }
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns null
        coEvery { animeEpisodeDetailRepository.insertCachedAnimeDetailComplement(any()) } just Runs
        coEvery { animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(any()) } just Runs
        var getAnimeAniwatchSearchCallCount = 0
        coEvery { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) } answers {
            getAnimeAniwatchSearchCallCount++
            if (getAnimeAniwatchSearchCallCount > 2) throw IllegalStateException("Excessive calls to getAnimeAniwatchSearch: $getAnimeAniwatchSearchCallCount")
            println("getAnimeAniwatchSearch called ($getAnimeAniwatchSearchCallCount) for title: Naruto: Shippuuden")
            Response.success(AnimeAniwatchSearchResponse(animes = listOf(animeAniwatchPlaceholder)))
        }
        var getEpisodesCallCount = 0
        coEvery { animeEpisodeDetailRepository.getEpisodes("anime-1735") } answers {
            getEpisodesCallCount++
            if (getEpisodesCallCount > 2) throw IllegalStateException("Excessive calls to getEpisodes: $getEpisodesCallCount")
            println("getEpisodes called ($getEpisodesCallCount) for animeId: anime-1735")
            Resource.Success(EpisodesResponse(totalEpisodes = 1, episodes = listOf(episodePlaceholder)))
        }
        var findClosestMatchesCallCount = 0
        coEvery {
            AnimeTitleFinder.findClosestMatches<AnimeAniwatch>(
                any(),
                listOf(animeAniwatchPlaceholder),
                any(),
                any()
            )
        } answers {
            findClosestMatchesCallCount++
            if (findClosestMatchesCallCount > 2) throw IllegalStateException("Excessive calls to findClosestMatches: $findClosestMatchesCallCount")
            println("findClosestMatches called ($findClosestMatchesCallCount) with titles: ${it.invocation.args[0]}")
            listOf(animeAniwatchPlaceholder)
        }
        var getEpisodeQueryCallCount = 0
        coEvery { StreamingUtils.getEpisodeQuery(any(), any()) } answers {
            getEpisodeQueryCallCount++
            if (getEpisodeQueryCallCount > 2) throw IllegalStateException("Excessive calls to getEpisodeQuery: $getEpisodeQueryCallCount")
            println("getEpisodeQuery called ($getEpisodeQueryCallCount) with args: ${it.invocation.args[0]}")
            episodeSourcesQueryPlaceholder
        }
        coEvery {
            animeEpisodeDetailRepository.updateCachedAnimeDetailComplementWithEpisodes(any(), any(), any())
        } answers {
            println("updateCachedAnimeDetailComplementWithEpisodes called with args: ${it.invocation.args[0]}")
            animeDetailComplementPlaceholder
        }
        var getEpisodeServersCallCount = 0
        coEvery { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") } answers {
            getEpisodeServersCallCount++
            if (getEpisodeServersCallCount > 2) throw IllegalStateException("Excessive calls to getEpisodeServers: $getEpisodeServersCallCount")
            println("getEpisodeServers called ($getEpisodeServersCallCount) for episodeId: lorem-ipsum-123?ep=123")
            Resource.Success(episodeServersResponsePlaceholder)
        }
        coEvery {
            animeEpisodeDetailRepository.getEpisodeSources("lorem-ipsum-123?ep=123", "vidstreaming", "sub")
        } answers {
            println("getEpisodeSources (repository) called for episodeId: lorem-ipsum-123?ep=123, server: vidstreaming, category: sub")
            Response.success(episodeSourcesResponsePlaceholder)
        }

        viewModel = AnimeDetailViewModel(animeEpisodeDetailRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkStatic("android.util.Log")
        unmockkStatic(System::class)
        unmockkObject(AnimeTitleFinder)
        unmockkObject(StreamingUtils)
        unmockkObject(FilterUtils)
    }

    @Test
    fun `loadAnimeDetail should update state with anime details and trigger loadEpisodes`() = runTest {
        val animeId = 1735

        try {
            viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
            advanceTimeBy(500)
            yield()
        } catch (e: StackOverflowError) {
            println("StackOverflowError in loadAnimeDetail")
            throw e
        }

        val state = viewModel.detailState.value
        println("AnimeDetail State: isSuccess=${state.animeDetail is Resource.Success}")
        println("AnimeDetailComplement State: isSuccess=${state.animeDetailComplement is Resource.Success}")
        if (state.animeDetailComplement is Resource.Error) {
            println("AnimeDetailComplement Error: ${state.animeDetailComplement.message}")
        }
        assertTrue("AnimeDetail should be Success", state.animeDetail is Resource.Success)
        assertTrue("AnimeDetailComplement should be Success", state.animeDetailComplement is Resource.Success)
        assertEquals("lorem-ipsum-123?ep=123", state.defaultEpisodeId)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(animeId) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodes("anime-1735") }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeServers("lorem-ipsum-123?ep=123") }
    }

    @Test
    fun `loadAnimeDetail should handle error and not trigger loadEpisodes`() = runTest {
        val animeId = 1735
        coEvery { animeEpisodeDetailRepository.getAnimeDetail(animeId) } returns Resource.Error("Network error")

        try {
            viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
            advanceTimeBy(500)
            yield()
        } catch (e: StackOverflowError) {
            println("StackOverflowError in loadAnimeDetail error case")
            throw e
        }

        val state = viewModel.detailState.value
        assertTrue("AnimeDetail should be Error", state.animeDetail is Resource.Error)
        assertEquals("Network error", (state.animeDetail as Resource.Error).message)
        assertTrue("AnimeDetailComplement should be Loading", state.animeDetailComplement is Resource.Loading)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(animeId) }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
    }

    @Test
    fun `loadRelationAnimeDetail should update relationAnimeDetails with anime detail`() = runTest {
        val relationId = 2
        val relationAnimeDetail = animeDetailPlaceholder.copy(mal_id = 2, title = "Related Anime")
        coEvery { animeEpisodeDetailRepository.getAnimeDetail(relationId) } returns Resource.Success(
            AnimeDetailResponse(data = relationAnimeDetail)
        )

        try {
            viewModel.onAction(DetailAction.LoadRelationAnimeDetail(relationId))
            advanceTimeBy(500)
            yield()
        } catch (e: StackOverflowError) {
            println("StackOverflowError in loadRelationAnimeDetail")
            throw e
        }

        val state = viewModel.detailState.value
        assertTrue("RelationAnimeDetails should contain id $relationId", state.relationAnimeDetails.containsKey(relationId))
        assertTrue("RelationAnimeDetails[$relationId] should be Success", state.relationAnimeDetails[relationId] is Resource.Success)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(relationId) }
    }

    @Test
    fun `loadEpisodeDetailComplement should use cached complement if available`() = runTest {
        val episodeId = episodeDetailComplementPlaceholder.id
        coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) } returns episodeDetailComplementPlaceholder
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns animeDetailComplementPlaceholder.copy(
            episodes = listOf(episodePlaceholder.copy(episodeId = episodeId))
        )

        try {
            viewModel.onAction(DetailAction.LoadAnimeDetail(1735))
            advanceTimeBy(500)
            yield()
            viewModel.onAction(DetailAction.LoadEpisodeDetailComplement(episodeId))
            advanceTimeBy(500)
            yield()
        } catch (e: StackOverflowError) {
            println("StackOverflowError in loadEpisodeDetailComplement cached case")
            throw e
        }

        val state = viewModel.detailState.value
        assertTrue("EpisodeDetailComplements should contain $episodeId", state.episodeDetailComplements.containsKey(episodeId))
        assertTrue("EpisodeDetailComplements[$episodeId] should be Success", state.episodeDetailComplements[episodeId] is Resource.Success)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getEpisodeServers(any()) }
    }

    @Test
    fun `loadEpisodeDetailComplement should fetch servers and sources if no cache`() = runTest {
        val episodeId = episodeDetailComplementPlaceholder.id
        val animeId = 1735
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns animeDetailComplementPlaceholder.copy(
            episodes = listOf(episodePlaceholder.copy(episodeId = episodeId))
        )
        coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) } returns null

        try {
            viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
            advanceUntilIdle()
            yield()
            coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) } returns null
            viewModel.onAction(DetailAction.LoadEpisodeDetailComplement(episodeId))
            advanceUntilIdle()
            yield()
        } catch (e: StackOverflowError) {
            println("StackOverflowError in loadEpisodeDetailComplement no cache case")
            throw e
        }

        val state = viewModel.detailState.value
        println("AnimeDetail State: isSuccess=${state.animeDetail is Resource.Success}")
        println("EpisodeDetailComplements State for $episodeId: isSuccess=${state.episodeDetailComplements[episodeId] is Resource.Success}")
        if (state.episodeDetailComplements[episodeId] is Resource.Error) {
            println("EpisodeDetailComplement Error: ${(state.episodeDetailComplements[episodeId] as Resource.Error).message}")
        } else if (state.episodeDetailComplements[episodeId] is Resource.Loading) {
            println("EpisodeDetailComplement is still Loading")
        }
        assertTrue("EpisodeDetailComplements should contain $episodeId", state.episodeDetailComplements.containsKey(episodeId))
        assertTrue("EpisodeDetailComplements[$episodeId] should be Success", state.episodeDetailComplements[episodeId] is Resource.Success)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeServers(episodeId) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getEpisodeSources(episodeId, "vidstreaming", "sub") }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(any()) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(animeId) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.updateCachedAnimeDetailComplementWithEpisodes(any(), any(), any()) }
    }

    @Test
    fun `loadEpisodes should use cached complement if available`() = runTest {
        val animeId = 1735
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(animeId) } returns animeDetailComplementPlaceholder.copy(
            episodes = listOf(episodePlaceholder.copy(episodeId = "lorem-ipsum-123?ep=123"))
        )
        coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement("lorem-ipsum-123?ep=123") } returns episodeDetailComplementPlaceholder

        try {
            viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
            advanceTimeBy(500)
            yield()
        } catch (e: StackOverflowError) {
            println("StackOverflowError in loadEpisodes cached case")
            throw e
        }

        val detailState = viewModel.detailState.value
        val filterState = viewModel.episodeFilterState.value
        assertTrue("AnimeDetailComplement should be Success", detailState.animeDetailComplement is Resource.Success)
        assertTrue("FilteredEpisodes should not be empty", filterState.filteredEpisodes.isNotEmpty())
        assertEquals("lorem-ipsum-123?ep=123", detailState.defaultEpisodeId)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(animeId) }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
    }

    @Test
    fun `loadEpisodes should handle music type anime correctly`() = runTest {
        val animeId = 1735
        val musicAnimeDetail = animeDetailPlaceholder.copy(mal_id = 1735, type = "Music")
        coEvery { animeEpisodeDetailRepository.getAnimeDetail(animeId) } returns Resource.Success(
            AnimeDetailResponse(data = musicAnimeDetail)
        )

        try {
            viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
            advanceTimeBy(500)
            yield()
        } catch (e: StackOverflowError) {
            println("StackOverflowError in loadEpisodes music case")
            throw e
        }

        val state = viewModel.detailState.value
        assertTrue("AnimeDetailComplement should be Success", state.animeDetailComplement is Resource.Success)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getAnimeDetail(animeId) }
        coVerify(exactly = 1) { animeEpisodeDetailRepository.insertCachedAnimeDetailComplement(any()) }
        coVerify(exactly = 0) { animeEpisodeDetailRepository.getAnimeAniwatchSearch(any()) }
    }

    @Test
    fun `updateEpisodeQueryState should filter episodes based on query`() = runTest {
        val animeId = 1735
        val query = FilterUtils.EpisodeQueryState(title = "Title of Episode")
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns animeDetailComplementPlaceholder.copy(
            episodes = listOf(episodePlaceholder)
        )
        coEvery { FilterUtils.filterEpisodes(any(), query, any()) } returns listOf(episodePlaceholder)

        try {
            viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
            advanceTimeBy(500)
            yield()
            viewModel.onAction(DetailAction.UpdateEpisodeQueryState(query))
            advanceTimeBy(500)
            yield()
        } catch (e: StackOverflowError) {
            println("StackOverflowError in updateEpisodeQueryState")
            throw e
        }

        val filterState = viewModel.episodeFilterState.value
        assertEquals(query, filterState.episodeQuery)
        assertTrue("FilteredEpisodes should not be empty", filterState.filteredEpisodes.isNotEmpty())
        coVerify(exactly = 1) { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) }
        coVerify(exactly = 1) { FilterUtils.filterEpisodes(any(), query, any()) }
    }

    @Test
    fun `toggleFavorite should update favorite status and cache`() = runTest {
        val animeId = 1735
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(1735) } returns animeDetailComplementPlaceholder
        coEvery { animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(any()) } just Runs

        try {
            viewModel.onAction(DetailAction.LoadAnimeDetail(animeId))
            advanceTimeBy(500)
            yield()
            viewModel.onAction(DetailAction.ToggleFavorite(true))
            advanceTimeBy(500)
            yield()
        } catch (e: StackOverflowError) {
            println("StackOverflowError in toggleFavorite")
            throw e
        }

        val state = viewModel.detailState.value
        assertTrue("AnimeDetailComplement should be Success", state.animeDetailComplement is Resource.Success)
        assertEquals(true, state.animeDetailComplement.data?.isFavorite == true)
        coVerify(exactly = 1) { animeEpisodeDetailRepository.updateCachedAnimeDetailComplement(any()) }
    }
}