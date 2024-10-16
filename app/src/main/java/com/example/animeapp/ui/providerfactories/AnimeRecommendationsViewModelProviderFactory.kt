package com.example.animeapp.ui.providerfactories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.animeapp.repository.AnimeRecommendationsRepository
import com.example.animeapp.ui.viewmodels.AnimeRecommendationsViewModel

class AnimeRecommendationsViewModelProviderFactory(
    val animeRecommendationsRepository: AnimeRecommendationsRepository
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AnimeRecommendationsViewModel(animeRecommendationsRepository) as T
    }
}