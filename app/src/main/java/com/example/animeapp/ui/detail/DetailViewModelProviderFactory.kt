package com.example.animeapp.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.animeapp.repository.AnimeDetailRepository
import com.example.animeapp.ui.home.DetailViewModel

class DetailViewModelProviderFactory(
    val animeDetailRepository: AnimeDetailRepository
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DetailViewModel(animeDetailRepository) as T
    }
}