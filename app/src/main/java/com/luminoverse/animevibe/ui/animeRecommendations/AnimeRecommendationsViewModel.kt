package com.luminoverse.animevibe.ui.animeRecommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminoverse.animevibe.models.AnimeRecommendationResponse
import com.luminoverse.animevibe.repository.AnimeRecommendationsRepository
import com.luminoverse.animevibe.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecommendationsState(
    val animeRecommendations: Resource<AnimeRecommendationResponse> = Resource.Loading(),
    val isRefreshing: Boolean = false,
)

sealed class RecommendationsAction {
    object LoadRecommendations : RecommendationsAction()
}

@HiltViewModel
class AnimeRecommendationsViewModel @Inject constructor(
    private val animeRecommendationsRepository: AnimeRecommendationsRepository
) : ViewModel() {

    private val _recommendationsState = MutableStateFlow(RecommendationsState())
    val recommendationsState: StateFlow<RecommendationsState> = _recommendationsState.asStateFlow()

    init {
        onAction(RecommendationsAction.LoadRecommendations)
    }

    fun onAction(action: RecommendationsAction) {
        when (action) {
            is RecommendationsAction.LoadRecommendations -> loadRecommendations()
        }
    }

    private fun loadRecommendations() = viewModelScope.launch {
        _recommendationsState.update {
            it.copy(
                isRefreshing = true,
                animeRecommendations = Resource.Loading()
            )
        }
        val result = animeRecommendationsRepository.getAnimeRecommendations()
        _recommendationsState.update {
            it.copy(
                animeRecommendations = result,
                isRefreshing = false
            )
        }
    }
}