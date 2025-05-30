package com.luminoverse.animevibe.data.remote.api

import com.luminoverse.animevibe.models.AnimeAniwatchSearchResponse
import com.luminoverse.animevibe.models.AnimeDetailResponse
import com.luminoverse.animevibe.models.AnimeRecommendationResponse
import com.luminoverse.animevibe.models.ListAnimeDetailResponse
import com.luminoverse.animevibe.models.AnimeSearchResponse
import com.luminoverse.animevibe.models.EpisodeServersResponse
import com.luminoverse.animevibe.models.EpisodeSourcesResponse
import com.luminoverse.animevibe.models.EpisodesResponse
import com.luminoverse.animevibe.models.GenresResponse
import com.luminoverse.animevibe.models.ProducerResponse
import com.luminoverse.animevibe.models.ProducersResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AnimeAPI {
    @GET("/v4/schedules")
    suspend fun getAnimeSchedules(
        @Query("filter") filter: String? = null,
        @Query("sfw") sfw: Boolean? = null,
        @Query("kids") kids: Boolean? = null,
        @Query("unapproved") unapproved: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<ListAnimeDetailResponse>

    @GET("/v4/top/anime")
    suspend fun getTop20Anime(
        @Query("filter") filter: String = "airing",
        @Query("sfw") sfw: Boolean = true,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ListAnimeDetailResponse>

    @GET("/v4/recommendations/anime")
    suspend fun getAnimeRecommendations(
        @Query("page") page: Int = 1,
    ): Response<AnimeRecommendationResponse>

    @GET("/v4/anime/{id}/full")
    suspend fun getAnimeDetail(
        @Path("id") id: Int
    ): Response<AnimeDetailResponse>

    @GET("/v4/random/anime")
    suspend fun getRandomAnime(
        @Query("sfw") sfw: Boolean = true
    ): Response<AnimeDetailResponse>

    @GET("/v4/anime")
    suspend fun getAnimeSearch(
        @Query("q") q: String,
        @Query("unapproved") unapproved: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("type") type: String? = null,
        @Query("score") score: Double? = null,
        @Query("min_score") minScore: Double? = null,
        @Query("max_score") maxScore: Double? = null,
        @Query("status") status: String? = null,
        @Query("rating") rating: String? = null,
        @Query("sfw") sfw: Boolean? = null,
        @Query("genres") genres: String? = null,
        @Query("genres_exclude") genresExclude: String? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("sort") sort: String? = null,
        @Query("letter") letter: String? = null,
        @Query("producers") producers: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<AnimeSearchResponse>

    @GET("/v4/genres/anime")
    suspend fun getGenres(): Response<GenresResponse>

    @GET("/v4/producers")
    suspend fun getProducers(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("q") q: String,
        @Query("order_by") orderBy: String? = null,
        @Query("sort") sort: String? = null,
        @Query("letter") letter: String? = null
    ): Response<ProducersResponse>

    @GET("/v4/producers/{id}")
    suspend fun getProducer(
        @Path("id") id: Int
    ): Response<ProducerResponse>

    @GET("/aniwatch/search")
    suspend fun getAnimeAniwatchSearch(
        @Query("keyword") keyword: String
    ): Response<AnimeAniwatchSearchResponse>

    @GET("/aniwatch/episodes/{id}")
    suspend fun getEpisodes(
        @Path("id") id: String
    ): Response<EpisodesResponse>

    @GET("/aniwatch/servers")
    suspend fun getEpisodeServers(
        @Query("id") episodeId: String
    ): Response<EpisodeServersResponse>

    @GET("/aniwatch/episode-srcs")
    suspend fun getEpisodeSources(
        @Query("id") episodeId: String,
        @Query("server") server: String,
        @Query("category") category: String
    ): Response<EpisodeSourcesResponse>
}