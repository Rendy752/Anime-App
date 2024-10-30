package com.example.animeappkotlin.data.remote.api

import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class MockCall<T>(private val response: T) : Call<T> {

    override fun enqueue(callback: Callback<T>) {
        callback.onResponse(this, Response.success(response))
    }

    override fun isExecuted(): Boolean {
        return false
    }

    override fun cancel() {
        // Do nothing - this is a mock
    }

    override fun isCanceled(): Boolean {
        return false
    }

    @Throws(IOException::class)
    override fun execute(): Response<T> {
        return Response.success(response)
    }

    override fun clone(): Call<T> {
        return this
    }

    override fun request(): Request? {
        return null
    }

    override fun timeout(): Timeout {
        TODO("Not yet implemented")
    }
}