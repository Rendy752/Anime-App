package com.example.animeapp.ui.animeDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.animeapp.repository.AnimeDetailRepository

class AnimeDetailViewModelProviderFactory(
    private val animeDetailRepository: AnimeDetailRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AnimeDetailViewModel(animeDetailRepository) as T
    }
}