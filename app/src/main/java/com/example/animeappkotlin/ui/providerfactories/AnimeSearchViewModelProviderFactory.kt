package com.example.animeappkotlin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.animeappkotlin.repository.AnimeSearchRepository

class AnimeSearchViewModelProviderFactory(
    private val animeSearchRepository: AnimeSearchRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnimeSearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnimeSearchViewModel(animeSearchRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}