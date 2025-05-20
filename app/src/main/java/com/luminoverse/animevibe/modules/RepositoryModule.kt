package com.luminoverse.animevibe.modules

import com.luminoverse.animevibe.data.local.dao.AnimeDetailComplementDao
import com.luminoverse.animevibe.data.local.dao.AnimeDetailDao
import com.luminoverse.animevibe.data.local.dao.EpisodeDetailComplementDao
import com.luminoverse.animevibe.data.local.dao.GenreDao
import com.luminoverse.animevibe.data.remote.api.AnimeAPI
import com.luminoverse.animevibe.di.AnimeRunwayApi
import com.luminoverse.animevibe.di.JikanApi
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.repository.AnimeRecommendationsRepository
import com.luminoverse.animevibe.repository.AnimeSearchRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object RepositoryModule {
    @Provides
    fun provideAnimeRecommendationsRepository(@JikanApi animeAPI: AnimeAPI): AnimeRecommendationsRepository {
        return AnimeRecommendationsRepository(animeAPI)
    }

    @Provides
    fun provideAnimeEpisodeDetailRepository(
        animeDetailDao: AnimeDetailDao,
        animeDetailComplementDao: AnimeDetailComplementDao,
        episodeDetailComplementDao: EpisodeDetailComplementDao,
        @JikanApi jikanAPI: AnimeAPI,
        @AnimeRunwayApi runwayAPI: AnimeAPI
    ): AnimeEpisodeDetailRepository {
        return AnimeEpisodeDetailRepository(
            animeDetailDao,
            animeDetailComplementDao,
            episodeDetailComplementDao,
            jikanAPI,
            runwayAPI
        )
    }

    @Provides
    fun provideAnimeSearchRepository(
        @JikanApi animeAPI: AnimeAPI,
        genreDao: GenreDao
    ): AnimeSearchRepository {
        return AnimeSearchRepository(animeAPI, genreDao)
    }
}