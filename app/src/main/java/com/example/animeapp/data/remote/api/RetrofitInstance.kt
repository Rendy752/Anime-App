package com.example.animeapp.data.remote.api

import com.example.animeapp.data.remote.logging.LogCollectorInterceptor
import com.example.animeapp.utils.Const.Companion.BASE_URL
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    internal val logCollectorInterceptor = LogCollectorInterceptor()

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logCollectorInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    val api: AnimeAPI by lazy {
        retrofit.create(AnimeAPI::class.java)
    }
}