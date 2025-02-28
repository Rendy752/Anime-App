package com.example.animeapp.modules

import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.di.AnimeRunwayApi
import com.example.animeapp.di.JikanApi
import com.example.animeapp.repository.AnimeDetailRepository
import com.example.animeapp.repository.AnimeRecommendationsRepository
import com.example.animeapp.repository.AnimeSearchRepository
import com.example.animeapp.repository.AnimeStreamingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object RepositoryModule {

    @Provides
    fun provideAnimeDetailRepository(
        animeDetailDao: AnimeDetailDao,
        @JikanApi animeAPI: AnimeAPI
    ): AnimeDetailRepository {
        return AnimeDetailRepository(animeDetailDao, animeAPI)
    }

    @Provides
    fun provideAnimeRecommendationsRepository(@JikanApi animeAPI: AnimeAPI): AnimeRecommendationsRepository {
        return AnimeRecommendationsRepository(animeAPI)
    }

    @Provides
    fun provideAnimeSearchRepository(@JikanApi animeAPI: AnimeAPI): AnimeSearchRepository {
        return AnimeSearchRepository(animeAPI)
    }

    @Provides
    fun provideAnimeStreamingRepository(@AnimeRunwayApi animeAPI: AnimeAPI): AnimeStreamingRepository {
        return AnimeStreamingRepository(animeAPI)
    }
}