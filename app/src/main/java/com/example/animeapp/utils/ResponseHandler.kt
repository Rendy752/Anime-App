package com.example.animeapp.utils

import retrofit2.Response

object ResponseHandler {
    fun <T> handleCommonResponse(response: Response<T>): Resource<T> {
        if (response.isSuccessful) {
            response.body()?.let { return Resource.Success(it) }
                ?: return Resource.Error("Response body is null")
        }
        return Resource.Error(response.message())
    }

    fun <T, R> handleResponse(
        response: Response<T>,
        onSuccess: (T) -> R?
    ): Resource<R> {
        if (response.isSuccessful) {
            val responseBody = response.body()
            if (responseBody != null) {
                val result = onSuccess(responseBody)
                return if (result != null) {
                    Resource.Success(result)
                } else {
                    Resource.Error("Failed to process response body")
                }
            } else {
                return Resource.Error("Response body is null")
            }
        } else {
            return Resource.Error(response.message())
        }
    }
}