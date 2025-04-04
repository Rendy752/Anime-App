package com.example.animeapp.modules

import com.example.animeapp.data.local.dao.AnimeDetailComplementDao
import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.local.dao.EpisodeDetailComplementDao
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.di.AnimeRunwayApi
import com.example.animeapp.di.JikanApi
import com.example.animeapp.repository.AnimeEpisodeDetailRepository
import com.example.animeapp.repository.AnimeRecommendationsRepository
import com.example.animeapp.repository.AnimeSearchRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object RepositoryModule {
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
    fun provideAnimeRecommendationsRepository(@JikanApi animeAPI: AnimeAPI): AnimeRecommendationsRepository {
        return AnimeRecommendationsRepository(animeAPI)
    }

    @Provides
    fun provideAnimeSearchRepository(@JikanApi animeAPI: AnimeAPI): AnimeSearchRepository {
        return AnimeSearchRepository(animeAPI)
    }
}