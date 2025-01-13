package com.example.animeappkotlin.ui.animeSearch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.animeappkotlin.data.remote.api.RetrofitInstance
import com.example.animeappkotlin.databinding.FragmentAnimeSearchBinding
import com.example.animeappkotlin.repository.AnimeSearchRepository

class AnimeSearchActivity : AppCompatActivity() {

    private lateinit var binding: FragmentAnimeSearchBinding
    private lateinit var viewModel: AnimeSearchViewModel

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