package com.example.animeappkotlin.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.animeappkotlin.data.remote.api.RetrofitInstance
import com.example.animeappkotlin.databinding.FragmentAnimeSearchBinding
import com.example.animeappkotlin.repository.AnimeSearchRepository
import com.example.animeappkotlin.ui.viewmodels.AnimeSearchViewModel
import com.example.animeappkotlin.ui.providerfactories.AnimeSearchViewModelProviderFactory

class AnimeSearchActivity : AppCompatActivity() {

    private lateinit var binding: FragmentAnimeSearchBinding
    lateinit var viewModel: AnimeSearchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentAnimeSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
    }

    private fun setupViewModel() {
        val animeSearchRepository = AnimeSearchRepository(api = RetrofitInstance.api)
        val factory = AnimeSearchViewModelProviderFactory(animeSearchRepository)
        viewModel = ViewModelProvider(this, factory)[AnimeSearchViewModel::class.java]
    }
}