package com.example.animeapp.data.remote.logging

import com.example.animeapp.models.LogEntry
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Field

class LogCollectorInterceptor : Interceptor {
    private val logEntries = mutableListOf<LogEntry>()
    val gson: Gson by lazy {
        val converterFactory = GsonConverterFactory.create()
        val gsonField: Field = GsonConverterFactory::class.java.getDeclaredField("gson")
        gsonField.isAccessible = true
        gsonField.get(converterFactory) as Gson
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val responseBody = response.body
        val responseBodyString = if (responseBody != null) {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer

            if (buffer.size == 0L) {
                "<empty_body>"
            } else {
                val jsonString = buffer.clone().readString(Charsets.UTF_8)
                try {
                    val je = JsonParser.parseString(jsonString)
                    gson.toJson(je)
                } catch (e: JsonSyntaxException) {
                    jsonString
                }
            }
        } else {
            "<null_body>"
        }

        if (responseBodyString != "<empty_body>" && responseBodyString != "<null_body>") {
            val logEntry = LogEntry(
                requestUrl = request.url.toString(),
                responseCode = response.code,
                responseBody = responseBodyString
            )
            logEntries.add(logEntry)
        }

        return response.newBuilder()
            .body(responseBodyString.toResponseBody(responseBody?.contentType()))
            .build()
    }

    fun getLogs(): List<LogEntry> {
        return logEntries
    }

    fun clearLogs() {
        logEntries.clear()
    }
}