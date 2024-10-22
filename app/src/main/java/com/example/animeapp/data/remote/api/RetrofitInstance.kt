package com.example.animeappkotlin.data.remote.api

import com.example.animeappkotlin.utils.Const.Companion.BASE_URL
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private var okHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private var retrofit: Retrofit

    init {
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    var api: AnimeAPI = retrofit.create(AnimeAPI::class.java)

    fun addInterceptor(interceptor: Interceptor) {
        if (!okHttpClient.interceptors.contains(interceptor)) {
            okHttpClient = okHttpClient.newBuilder()
                .addInterceptor(interceptor)
                .build()

            retrofit = retrofit.newBuilder()
                .client(okHttpClient)
                .build()

            api = retrofit.create(AnimeAPI::class.java)
        }
    }
}