package com.example.animeapp.data.remote.api

import android.content.Context
import com.example.animeapp.utils.Const.Companion.JIKAN_URL
import com.example.animeapp.utils.Const.Companion.ANIMERUNWAY_URL
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitInstance @Inject constructor(
    okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) {

    private var jikanRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(JIKAN_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val jikanApi: AnimeAPI by lazy { jikanRetrofit.create(AnimeAPI::class.java) }

    private var animeRunwayRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ANIMERUNWAY_URL)
        .client(provideCachingOkHttpClient(provideCache(context)))
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val animeRunwayApi: AnimeAPI by lazy { animeRunwayRetrofit.create(AnimeAPI::class.java) }

    private fun provideCachingOkHttpClient(cache: Cache): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            .build()
    }

    private fun provideCache(context: Context): Cache {
        val cacheSize = 10 * 1024 * 1024
        val httpCacheDirectory = File(context.cacheDir, "http-cache")
        return Cache(httpCacheDirectory, cacheSize.toLong())
    }
}