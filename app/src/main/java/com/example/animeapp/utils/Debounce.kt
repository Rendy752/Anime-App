package com.example.animeapp.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

class Debounce<T>(
    private val coroutineScope: CoroutineScope,
    private val delayMillis: Long = 1000L,
    private val onDebounced: (T) -> Unit
) {

    private var searchJob: Job? = null

    fun query(value: T) {
        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            delay(delayMillis)
            onDebounced(value)
        }
    }
}