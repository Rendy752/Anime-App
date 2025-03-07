package com.example.animeapp.modules

import android.content.Context
import com.example.animeapp.data.local.dao.AnimeDetailComplementDao
import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.local.dao.EpisodeDetailComplementDao
import com.example.animeapp.data.local.database.AnimeDetailComplementDatabase
import com.example.animeapp.data.local.database.AnimeDetailDatabase
import com.example.animeapp.data.local.database.EpisodeDetailComplementDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAnimeDetailDatabase(@ApplicationContext context: Context): AnimeDetailDatabase {
        return AnimeDetailDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideAnimeDetailDao(database: AnimeDetailDatabase): AnimeDetailDao {
        return database.getAnimeDetailDao()
    }

    @Provides
    @Singleton
    fun provideAnimeDetailComplementDatabase(@ApplicationContext context: Context): AnimeDetailComplementDatabase {
        return AnimeDetailComplementDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideAnimeDetailComplementDao(database: AnimeDetailComplementDatabase): AnimeDetailComplementDao {
        return database.getAnimeDetailComplementDao()
    }

    @Provides
    @Singleton
    fun provideEpisodeDetailComplementDatabase(@ApplicationContext context: Context): EpisodeDetailComplementDatabase {
        return EpisodeDetailComplementDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideEpisodeDetailComplementDao(database: EpisodeDetailComplementDatabase): EpisodeDetailComplementDao {
        return database.getEpisodeDetailComplementDao()
    }
}