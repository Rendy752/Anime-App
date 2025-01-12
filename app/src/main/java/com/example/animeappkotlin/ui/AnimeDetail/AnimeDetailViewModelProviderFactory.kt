package com.example.animeappkotlin.ui.AnimeDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.animeappkotlin.repository.AnimeDetailRepository

class AnimeDetailViewModelProviderFactory(
    val animeDetailRepository: AnimeDetailRepository
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AnimeDetailViewModel(animeDetailRepository) as T
    }
}