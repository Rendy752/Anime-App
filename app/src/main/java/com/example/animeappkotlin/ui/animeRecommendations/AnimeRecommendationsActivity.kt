package com.example.animeappkotlin.ui.animeRecommendations

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.animeappkotlin.data.remote.api.RetrofitInstance
import com.example.animeappkotlin.databinding.FragmentRecommendationBinding
import com.example.animeappkotlin.repository.AnimeRecommendationsRepository

class AnimeRecommendationsActivity : AppCompatActivity() {

    private lateinit var binding: FragmentRecommendationBinding
    private lateinit var viewModel: AnimeRecommendationsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentRecommendationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
    }

    private fun setupViewModel() {
        val animeRecommendationsRepository = AnimeRecommendationsRepository(
            RetrofitInstance.api
        )
        val factory = AnimeRecommendationsViewModelProviderFactory(animeRecommendationsRepository)
        viewModel = ViewModelProvider(this, factory)[AnimeRecommendationsViewModel::class.java]
    }
}