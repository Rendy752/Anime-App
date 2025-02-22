package com.example.animeapp.utils

import com.example.animeapp.ui.animeSearch.AnimeSearchViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

class Debounce(
    private val coroutineScope: CoroutineScope,
    private val delayMillis: Long = 1000L,
    private val onDebounced: (String) -> Unit,
    private val viewModel: AnimeSearchViewModel
) {

    private var searchJob: Job? = null
    private var lastText: String? = null

    fun query(text: String) {
        lastText = text
        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            delay(delayMillis)
            if (lastText == text && text != viewModel.queryState.value.query) {
                onDebounced(text)
            }
        }
    }
}