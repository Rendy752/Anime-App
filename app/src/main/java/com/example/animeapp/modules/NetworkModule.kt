package com.example.animeapp.modules

import android.content.Context
import com.example.animeapp.BuildConfig.DEBUG
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.data.remote.api.EpisodeSourcesCacheInterceptor
import com.example.animeapp.data.remote.api.RetrofitInstance
import com.example.animeapp.di.AnimeRunwayApi
import com.example.animeapp.di.JikanApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .cache(provideCache(context))
            .addInterceptor(EpisodeSourcesCacheInterceptor())

        if (DEBUG) {
            val chuckerCollector = ChuckerCollector(
                context = context,
                showNotification = true,
                retentionPeriod = RetentionManager.Period.ONE_HOUR
            )
            val chuckerInterceptor = ChuckerInterceptor.Builder(context).collector(chuckerCollector)
                .maxContentLength(250_000L).redactHeaders("Auth-Token", "Bearer")
                .alwaysReadResponseBody(true).createShortcut(true).build()
            builder.addInterceptor(chuckerInterceptor)
        }

        return builder.build()
    }

    private fun provideCache(context: Context): Cache {
        val cacheSize = 10 * 1024 * 1024
        val httpCacheDirectory = File(context.cacheDir, "http-cache")
        return Cache(httpCacheDirectory, cacheSize.toLong())
    }

    @Provides
    @Singleton
    fun provideRetrofitInstance(okHttpClient: OkHttpClient): RetrofitInstance {
        return RetrofitInstance(okHttpClient)
    }

    @Provides
    @Singleton
    @JikanApi
    fun provideJikanAPI(retrofitInstance: RetrofitInstance): AnimeAPI {
        return retrofitInstance.jikanApi
    }

    @Provides
    @Singleton
    @AnimeRunwayApi
    fun provideAnimeRunwayAPI(retrofitInstance: RetrofitInstance): AnimeAPI {
        return retrofitInstance.animeRunwayApi
    }
}