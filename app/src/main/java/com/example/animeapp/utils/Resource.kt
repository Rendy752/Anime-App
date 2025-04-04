package com.example.animeapp.utils

sealed class Resource<T>(
    open val data: T? = null,
    val message: String? = null
) {
    data class Success<T>(override val data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T> : Resource<T>()
}