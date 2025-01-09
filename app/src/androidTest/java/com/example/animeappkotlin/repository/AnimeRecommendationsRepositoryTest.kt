package com.example.animeappkotlin.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.animeappkotlin.data.local.database.AnimeRecommendationsDatabase
import com.example.animeappkotlin.data.remote.api.AnimeAPI
import com.example.animeappkotlin.data.remote.api.RetrofitInstance
import com.example.animeappkotlin.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.*
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class AnimeRecommendationsRepositoryTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: AnimeRecommendationsRepository
    private lateinit var animeAPI: AnimeAPI
    private lateinit var database: AnimeRecommendationsDatabase
    private val json = Json { ignoreUnknownKeys = true } // Create a Json instance

    @Before
    fun setup() {
        animeAPI = RetrofitInstance.api;

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
    fun performanceTest() = runTest {
        val iterations = 10
        val serializationTimes = LongArray(iterations)
        val deserializationTimes = LongArray(iterations)


        val realApiResponse: Response<AnimeRecommendationResponse> =
            animeAPI.getAnimeRecommendations(1)
        val realResponseData = realApiResponse.body()

        if (realResponseData != null) {
            // Warm-up
//            repeat(3) {
//                measureSerializationTime(realResponseData)
//                measureDeserializationTime(realResponseData)
//            }
            repeat(iterations) { i ->
                serializationTimes[i] = measureSerializationTime(realResponseData)
                deserializationTimes[i] = measureDeserializationTime(realResponseData)
                delay(1000)
            }

            println("\nResults:")
            println("Serialization Times: ${serializationTimes.contentToString()}")
            println("Deserialization Times: ${deserializationTimes.contentToString()}")

            println("\nMeans:")
            println("Mean Serialization Time: ${serializationTimes.average()} ms")
            println("Mean Deserialization Time: ${deserializationTimes.average()} ms")
        } else {
            println("Error fetching real API response")
        }
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

    private fun measureSerializationTime(data: AnimeRecommendationResponse): Long {
        val startTime = System.currentTimeMillis()
        json.encodeToString(AnimeRecommendationResponse.serializer(), data)
        val endTime = System.currentTimeMillis()
        return endTime - startTime
    }

    private fun measureDeserializationTime(data: AnimeRecommendationResponse): Long {
        val jsonString = json.encodeToString(AnimeRecommendationResponse.serializer(), data)
        val startTime = System.currentTimeMillis()
        json.decodeFromString(
            AnimeRecommendationResponse.serializer(),
            jsonString
        )
        val endTime = System.currentTimeMillis()
        return endTime - startTime
    }
}