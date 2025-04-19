package com.example.animeapp.animeEpisodeDetail

import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.local.dao.AnimeDetailComplementDao
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeAniwatchSearchResponse
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.models.EpisodesResponse
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.TimeUtils
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AnimeEpisodeDetailRepositoryTest {

    private lateinit var animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
    private val animeDetailDao: AnimeDetailDao = mockk()
    private val animeDetailComplementDao: AnimeDetailComplementDao = mockk()

    @Before
    fun setup() {
        animeEpisodeDetailRepository = mockk()
    }

    private fun setupAnimeDetailMocks(lastEpisodeUpdatedAt: Long): Triple<AnimeDetail, AnimeDetailResponse, AnimeDetailComplement> {
        val zoneId = ZoneId.of("Asia/Tokyo")
        val currentTime =
            LocalDateTime.ofInstant(Instant.ofEpochSecond(lastEpisodeUpdatedAt), zoneId)
        val nextHour = currentTime.plusHours(1)

        val broadcastTime = nextHour.format(DateTimeFormatter.ofPattern("HH:mm"))
        val broadcastDay = nextHour.dayOfWeek.toString()

        val animeDetail = mockk<AnimeDetail> {
            every { airing } returns true
            every { broadcast.time } returns broadcastTime
            every { broadcast.timezone } returns "Asia/Tokyo"
            every { broadcast.day } returns broadcastDay
            every { mal_id } returns 1735
        }

        val animeDetailResponse = AnimeDetailResponse(animeDetail)
        val animeDetailComplement = mockk<AnimeDetailComplement>()

        return Triple(animeDetail, animeDetailResponse, animeDetailComplement)
    }

    @Test
    fun `isEpisodeAreUpToDate should return true when episode is up to date`() {
        val lastEpisodeUpdatedAt = Instant.now().epochSecond

        val (animeDetail) = setupAnimeDetailMocks(lastEpisodeUpdatedAt)

        val isUpToDate = TimeUtils.isEpisodeAreUpToDate(
            animeDetail.broadcast.time,
            animeDetail.broadcast.timezone,
            animeDetail.broadcast.day,
            lastEpisodeUpdatedAt
        )

        assertEquals(true, isUpToDate)
    }

    @Test
    fun `getAnimeDetail should return cached data when available and up to date`() = runBlocking {
        val animeId = 1735
        val lastEpisodeUpdatedAt = Instant.now().epochSecond

        val (animeDetail, animeDetailResponse) = setupAnimeDetailMocks(lastEpisodeUpdatedAt)

        val animeDetailComplementMock = mockk<AnimeDetailComplement>()
        every { animeDetailComplementMock.lastEpisodeUpdatedAt } returns lastEpisodeUpdatedAt

        every { animeDetailDao.getAnimeDetailById(animeId) } returns animeDetail
        every { animeDetailComplementDao.getAnimeDetailComplementByMalId(animeId) } returns animeDetailComplementMock

        coEvery { animeEpisodeDetailRepository.getAnimeDetail(animeId) } returns Resource.Success(
            animeDetailResponse
        )

        val result = animeEpisodeDetailRepository.getAnimeDetail(animeId)

        assertEquals(Resource.Success(animeDetailResponse), result)
    }

    @Test
    fun `getAnimeDetail should return remote data when cache is not available`() = runBlocking {
        val animeId = 123
        val mockAnimeDetail = mockk<AnimeDetail>()
        val animeDetailResponse = AnimeDetailResponse(mockAnimeDetail)
        val expectedResponse = Resource.Success(animeDetailResponse)

        coEvery { animeEpisodeDetailRepository.getAnimeDetail(animeId) } returns expectedResponse
        val result = animeEpisodeDetailRepository.getAnimeDetail(animeId)

        assertEquals(expectedResponse, result)
    }

    @Test
    fun `getCachedAnimeDetailComplementByMalId should return cached data`() = runBlocking {
        val malId = 123
        val animeDetailComplement = mockk<AnimeDetailComplement>()
        coEvery { animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(malId) } returns animeDetailComplement

        val result = animeEpisodeDetailRepository.getCachedAnimeDetailComplementByMalId(malId)

        assertEquals(animeDetailComplement, result)
    }

    @Test
    fun `insertCachedAnimeDetailComplement should insert data`() = runBlocking {
        val animeDetailComplement = mockk<AnimeDetailComplement>()
        coEvery {
            animeEpisodeDetailRepository.insertCachedAnimeDetailComplement(
                animeDetailComplement
            )
        } returns Unit

        animeEpisodeDetailRepository.insertCachedAnimeDetailComplement(animeDetailComplement)
    }

    @Test
    fun `updateAnimeDetailComplementWithEpisodes should return updated data when needed and successful`() =
        runBlocking {
            val animeDetail = mockk<AnimeDetail> {
                every { airing } returns true
                every { broadcast.time } returns "12:00"
                every { broadcast.timezone } returns "UTC"
                every { broadcast.day } returns "MONDAY"
                every { mal_id } returns 123
            }
            val animeDetailComplement = mockk<AnimeDetailComplement>()
            coEvery {
                animeEpisodeDetailRepository.updateAnimeDetailComplementWithEpisodes(
                    any(),
                    any()
                )
            } returns animeDetailComplement

            val result = animeEpisodeDetailRepository.updateAnimeDetailComplementWithEpisodes(
                animeDetail,
                animeDetailComplement
            )

            assertEquals(animeDetailComplement, result)
        }

    @Test
    fun `getAnimeDetail should return remote data when cache is outdated`() = runBlocking {
        val animeId = 1735
        val lastEpisodeUpdatedAt = Instant.now().epochSecond - 3600

        val (animeDetail, animeDetailResponse) = setupAnimeDetailMocks(lastEpisodeUpdatedAt)

        val animeDetailComplementMock = mockk<AnimeDetailComplement>()
        every { animeDetailComplementMock.lastEpisodeUpdatedAt } returns lastEpisodeUpdatedAt

        val animeDetailDao = mockk<AnimeDetailDao> {
            coEvery { getAnimeDetailById(animeId) } returns animeDetail
            coEvery { updateAnimeDetail(any()) } returns Unit
            coEvery { insertAnimeDetail(any()) } returns Unit
        }

        val animeDetailComplementDao = mockk<AnimeDetailComplementDao> {
            coEvery { getAnimeDetailComplementByMalId(animeId) } returns animeDetailComplementMock
        }

        val jikanAPI = mockk<AnimeAPI> {
            coEvery { getAnimeDetail(animeId) } returns Response.success(animeDetailResponse)
        }
        val runwayAPI = mockk<AnimeAPI>()

        val repository = AnimeEpisodeDetailRepository(
            animeDetailDao,
            animeDetailComplementDao,
            mockk(),
            jikanAPI,
            runwayAPI
        )

        val result = repository.getAnimeDetail(animeId)

        assertEquals(Resource.Success(animeDetailResponse), result)
    }

    @Test
    fun `updateAnimeDetailComplementWithEpisodes should return cached data when not needed`() =
        runBlocking {
            val (animeDetail, _, animeDetailComplement) = setupAnimeDetailMocks(Instant.now().epochSecond)

            coEvery {
                animeEpisodeDetailRepository.updateAnimeDetailComplementWithEpisodes(
                    any(),
                    any()
                )
            } returns animeDetailComplement

            val result = animeEpisodeDetailRepository.updateAnimeDetailComplementWithEpisodes(
                animeDetail,
                animeDetailComplement
            )

            assertEquals(animeDetailComplement, result)
        }

    @Test
    fun `updateAnimeDetailComplementWithEpisodes should return cached object when api call fails`() =
        runBlocking {
            val (animeDetail, _, animeDetailComplement) = setupAnimeDetailMocks(Instant.now().epochSecond)

            coEvery {
                animeEpisodeDetailRepository.updateAnimeDetailComplementWithEpisodes(
                    any(),
                    any()
                )
            } returns animeDetailComplement

            val result = animeEpisodeDetailRepository.updateAnimeDetailComplementWithEpisodes(
                animeDetail,
                animeDetailComplement
            )

            assertEquals(animeDetailComplement, result)
        }

    @Test
    fun `getCachedEpisodeDetailComplement should return cached data`() = runBlocking {
        val episodeId = "123"
        val episodeDetailComplement = mockk<EpisodeDetailComplement>()
        coEvery { animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId) } returns episodeDetailComplement

        val result = animeEpisodeDetailRepository.getCachedEpisodeDetailComplement(episodeId)

        assertEquals(episodeDetailComplement, result)
    }

    @Test
    fun `insertCachedEpisodeDetailComplement should insert data`() = runBlocking {
        val episodeDetailComplement = mockk<EpisodeDetailComplement>()
        coEvery {
            animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(
                episodeDetailComplement
            )
        } returns Unit

        animeEpisodeDetailRepository.insertCachedEpisodeDetailComplement(episodeDetailComplement)
    }

    @Test
    fun `updateEpisodeDetailComplement should update data`() = runBlocking {
        val episodeDetailComplement = mockk<EpisodeDetailComplement>()
        coEvery { animeEpisodeDetailRepository.updateEpisodeDetailComplement(episodeDetailComplement) } returns Unit

        animeEpisodeDetailRepository.updateEpisodeDetailComplement(episodeDetailComplement)
    }

    @Test
    fun `getAnimeAniwatchSearch should trigger api call`() {
        runBlocking {
            val keyword = "test"
            val expectedResponse = Response.success(mockk<AnimeAniwatchSearchResponse>())
            coEvery { animeEpisodeDetailRepository.getAnimeAniwatchSearch(keyword) } returns expectedResponse

            val result = animeEpisodeDetailRepository.getAnimeAniwatchSearch(keyword)

            assertEquals(expectedResponse, result)
        }
    }

    @Test
    fun `getEpisodes should trigger api call`() {
        runBlocking {
            val id = "123"
            val expectedResponse = Resource.Success(mockk<EpisodesResponse>())

            coEvery { animeEpisodeDetailRepository.getEpisodes(id) } returns expectedResponse
            val result = animeEpisodeDetailRepository.getEpisodes(id)

            assertEquals(expectedResponse, result)
        }
    }

    @Test
    fun `getEpisodeServers should trigger api call`() {
        runBlocking {
            val episodeId = "123"
            val expectedResponse = Resource.Success(mockk<EpisodeServersResponse>())
            coEvery { animeEpisodeDetailRepository.getEpisodeServers(episodeId) } returns expectedResponse
            val result = animeEpisodeDetailRepository.getEpisodeServers(episodeId)

            assertEquals(expectedResponse, result)
        }
    }

    @Test
    fun `getEpisodeSources should trigger api call`() {
        runBlocking {
            val episodeId = "123"
            val server = "server1"
            val category = "category1"
            val expectedResponse = Response.success(mockk<EpisodeSourcesResponse>())
            coEvery {
                animeEpisodeDetailRepository.getEpisodeSources(
                    episodeId,
                    server,
                    category
                )
            } returns expectedResponse

            val result = animeEpisodeDetailRepository.getEpisodeSources(episodeId, server, category)

            assertEquals(expectedResponse, result)
        }
    }
}