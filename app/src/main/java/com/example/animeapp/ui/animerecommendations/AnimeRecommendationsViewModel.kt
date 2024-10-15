package com.example.animeapp.ui.animerecommendations

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.animeapp.repository.AnimeRecommendationsRepository

class AnimeRecommendationsViewModel(
    private val animeRecommendationsRepository: AnimeRecommendationsRepository
): ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "This is recommendation Fragment"
    }
    val text: LiveData<String> = _text
}