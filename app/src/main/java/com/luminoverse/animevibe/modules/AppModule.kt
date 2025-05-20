package com.luminoverse.animevibe.modules

import com.luminoverse.animevibe.data.remote.api.AnimeAPI
import com.luminoverse.animevibe.di.JikanApi
import com.luminoverse.animevibe.repository.AnimeHomeRepository
import com.luminoverse.animevibe.utils.NotificationHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideNotificationHandler(): NotificationHandler {
        return NotificationHandler()
    }

    @Provides
    @Singleton
    fun provideAnimeHomeRepository(@JikanApi animeAPI: AnimeAPI): AnimeHomeRepository {
        return AnimeHomeRepository(animeAPI)
    }
}