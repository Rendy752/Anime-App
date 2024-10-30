package com.example.animeappkotlin.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.animeappkotlin.data.local.database.AnimeRecommendationsDatabase
import com.example.animeappkotlin.data.remote.api.AnimeAPI
import com.example.animeappkotlin.data.remote.api.MockAnimeAPI
import com.example.animeappkotlin.models.*
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimeRecommendationsRepositoryTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: AnimeRecommendationsRepository
    private lateinit var animeAPI: AnimeAPI
    private lateinit var database: AnimeRecommendationsDatabase
    private lateinit var mockResponse: AnimeRecommendationResponse
    private lateinit var gson: Gson

    @Before
    fun setup() {
        mockResponse = createMockResponse()
        animeAPI = MockAnimeAPI(mockResponse) // Inject MockAnimeAPI for testing
        gson = Gson()

        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AnimeRecommendationsDatabase::class.java
        ).allowMainThreadQueries().build()

        // Inject dependencies into the repository
        repository = AnimeRecommendationsRepository(animeAPI, database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testGetAnimeRecommendations() = runTest {
        val response = animeAPI.getAnimeRecommendations(1)
        Assert.assertEquals(mockResponse, response.body())
    }

    @Test
    fun repository_getAnimeRecommendations_returnsData() = runTest {
        val response = repository.getAnimeRecommendations(1)
        Assert.assertNotNull(response)
    }

    @Test
    fun performanceTest() = runTest {
        val iterations = 10
        val executionTimes = LongArray(iterations)
        val deserializationTimes = LongArray(iterations)
        val cpuUsages = DoubleArray(iterations)
        val memoryUsages = LongArray(iterations)

        repeat(iterations) { i ->
            executionTimes[i] = measureApiConsumptionTime()
            delay(1000) // Delay between tests
            deserializationTimes[i] = measureDeserializationTime()
            delay(1000) // Delay between tests
            cpuUsages[i] = measureCpuUsageDuringApiConsumption()
            delay(1000) // Delay between tests
            memoryUsages[i] = measureMemoryUsageDuringApiConsumption()
        }

        // Display results and calculate means
        println("\nResults:")
        println("Execution Times: ${executionTimes.contentToString()}")
        println("Deserialization Times: ${deserializationTimes.contentToString()}")
        println("CPU Usages (Disclaimer: Might not be accurate): ${cpuUsages.contentToString()}")
        println("Memory Usages (Disclaimer: Might not be accurate): ${memoryUsages.contentToString()}")

        println("\nMeans:")
        println("Mean Execution Time: ${executionTimes.average()} ms")
        println("Mean Deserialization Time: ${deserializationTimes.average()} ms")
        println("Mean CPU Usage (Disclaimer: Might not be accurate): ${cpuUsages.average()} %")
        println("Mean Memory Usage (Disclaimer: Might not be accurate): ${memoryUsages.average()} bytes")
    }

    private fun createMockResponse(): AnimeRecommendationResponse {
        val pagination = Pagination(1, true)
        val data = listOf(
            AnimeRecommendation(
                "1",
                listOf(
                    AnimeHeader(
                        38524,
                        "https://myanimelist.net/anime/38524/Shingeki_no_Kyojin__The_Final_Season",
                        Images(
                            ImageUrl(
                                "https://cdn.myanimelist.net/images/anime/1965/126125.jpg",
                                "https://cdn.myanimelist.net/images/anime/1965/126125t.jpg",
                                null,
                                "https://cdn.myanimelist.net/images/anime/1965/126125l.jpg",
                                null
                            ),
                            ImageUrl(
                                "https://cdn.myanimelist.net/images/anime/1965/126125.webp",
                                "https://cdn.myanimelist.net/images/anime/1965/126125t.webp",
                                null,
                                "https://cdn.myanimelist.net/images/anime/1965/126125l.webp",
                                null
                            )
                        ),
                        "Shingeki no Kyojin: The Final Season"
                    )
                ),
                "Amazing anime, highly recommended!",
                "2023-10-27T10:00:00.000Z",
                User(
                    "testuser",
                    "https://myanimelist.net/profile/testuser"
                )
            )
        )
        return AnimeRecommendationResponse(pagination, data)
    }

    private fun measureApiConsumptionTime(): Long = runBlocking {
        val startTime = System.currentTimeMillis()
        repository.getAnimeRecommendations(1)
        val endTime = System.currentTimeMillis()
        println("API Consumption Time: ${endTime - startTime} ms")

        return@runBlocking endTime - startTime
    }

    private fun measureDeserializationTime(): Long {
        val startTime = System.currentTimeMillis()
        val jsonResponse = gson.toJson(mockResponse)
        gson.fromJson(jsonResponse, AnimeRecommendationResponse::class.java)
        val endTime = System.currentTimeMillis()
        println("Deserialization Time: ${endTime - startTime} ms")

        return endTime - startTime
    }

    private fun measureCpuUsageDuringApiConsumption(): Double {
        val cpuBefore = getCpuUsage()
        runBlocking { repository.getAnimeRecommendations(1) } // Simulate refresh
        val cpuAfter = getCpuUsage()
        return cpuAfter - cpuBefore
    }

    private fun measureMemoryUsageDuringApiConsumption(): Long {
        val memoryBefore = getMemoryUsage()
        runBlocking { repository.getAnimeRecommendations(1) } // Simulate refresh
        val memoryAfter = getMemoryUsage()
        return memoryAfter - memoryBefore
    }

    private fun getCpuUsage(): Double {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val usedMemory = totalMemory - runtime.freeMemory()
        return (usedMemory.toDouble() / totalMemory) * 100 // Approximate CPU usage
    }

    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
}