package com.example.animeapp.data.remote.logging

import okhttp3.Interceptor
import okhttp3.Response

class LogCollectorInterceptor : Interceptor {
    private val logBuffer = mutableListOf<String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val logMessage = "Request: ${request.url}\nResponse: ${response.code}"
        logBuffer.add(logMessage)

        return response
    }

    fun getLogs(): String {
        return logBuffer.joinToString("\n")
    }

    fun clearLogs() {
        logBuffer.clear()
    }
}