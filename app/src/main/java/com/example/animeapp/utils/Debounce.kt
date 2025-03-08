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
    private val viewModel: AnimeSearchViewModel? = null,
    private val stateType: StateType? = null
) {

    private var searchJob: Job? = null

    enum class StateType {
        ANIME_SEARCH,
        PRODUCER_SEARCH
    }

    fun query(text: String) {
        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            delay(delayMillis)

            if (viewModel != null && stateType != null) {
                val shouldExecute = when (stateType) {
                    StateType.ANIME_SEARCH -> text != viewModel.queryState.value.query
                    StateType.PRODUCER_SEARCH -> text != viewModel.producersQueryState.value.query
                }

                if (shouldExecute) {
                    onDebounced(text)
                }
            } else {
                onDebounced(text)
            }
        }
    }
}