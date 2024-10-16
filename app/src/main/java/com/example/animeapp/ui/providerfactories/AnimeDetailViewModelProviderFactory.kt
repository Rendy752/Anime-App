package com.example.animeapp.ui.providerfactories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.animeapp.repository.AnimeDetailRepository
import com.example.animeapp.ui.viewmodels.AnimeDetailViewModel

class AnimeDetailViewModelProviderFactory(
    val animeDetailRepository: AnimeDetailRepository
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AnimeDetailViewModel(animeDetailRepository) as T
    }
}