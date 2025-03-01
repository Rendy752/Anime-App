package com.example.animeapp.data.remote.api

import com.example.animeapp.utils.Const.Companion.JIKAN_URL
import com.example.animeapp.utils.Const.Companion.ANIMERUNWAY_URL
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitInstance @Inject constructor(
    okHttpClient: OkHttpClient
) {

    private var jikanRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(JIKAN_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val jikanApi: AnimeAPI by lazy { jikanRetrofit.create(AnimeAPI::class.java) }

    private var animeRunwayRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ANIMERUNWAY_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val animeRunwayApi: AnimeAPI by lazy { animeRunwayRetrofit.create(AnimeAPI::class.java) }
}