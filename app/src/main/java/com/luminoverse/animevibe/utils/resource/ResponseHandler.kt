package com.luminoverse.animevibe.utils.resource

import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

object ResponseHandler {
    fun <T> handleCommonResponse(response: Response<T>): Resource<T> {
        if (response.isSuccessful) {
            response.body()?.let { return Resource.Success(it) }
                ?: return Resource.Error("Response body is null")
        }
        return Resource.Error(response.errorBody()?.string() ?: "Unknown error")
    }

    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Response<T> {
        return try {
            val response = apiCall.invoke()
            if (response.isSuccessful) {
                response
            } else {
                response
            }
        } catch (_: IOException) {
            Response.error(500, "Network error".toResponseBody())
        } catch (e: HttpException) {
            Response.error(e.code(), "HTTP error".toResponseBody())
        } catch (_: Exception) {
            Response.error(500, "Unknown error".toResponseBody())
        }
    }
}