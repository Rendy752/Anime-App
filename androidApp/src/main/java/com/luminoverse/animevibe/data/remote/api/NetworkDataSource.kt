package com.luminoverse.animevibe.data.remote.api

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    /**
     * Fetches text content from a given URL using the application's shared OkHttpClient.
     * This ensures all network requests benefit from custom configurations like the IPv6 fix.
     *
     * @param url The URL to fetch content from.
     * @return The text content as a String, or null if the request fails.
     */
    suspend fun fetchText(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            } catch (_: IOException) {
                null
            }
        }
    }
}