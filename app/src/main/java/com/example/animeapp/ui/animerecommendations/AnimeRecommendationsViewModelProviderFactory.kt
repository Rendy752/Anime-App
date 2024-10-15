package com.example.animeapp.ui.animerecommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.animeapp.repository.AnimeRecommendationsRepository

class AnimeRecommendationsViewModelProviderFactory(
    val animeRecommendationsRepository: AnimeRecommendationsRepository
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AnimeRecommendationsViewModel(animeRecommendationsRepository) as T
    }
}