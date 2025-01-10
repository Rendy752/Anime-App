package com.example.animeappkotlin.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

class Debouncer(
    private val coroutineScope: CoroutineScope,
    private val delayMillis: Long = 500L,
    private val onDebounced: (String) -> Unit
) {

    private var searchJob: Job? = null

    fun query(text: String) {
        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            delay(delayMillis)
            onDebounced(text)
        }
    }
}