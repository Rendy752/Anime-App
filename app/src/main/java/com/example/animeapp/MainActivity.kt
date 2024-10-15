package com.example.animeapp

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.animeapp.databinding.ActivityMainBinding
import com.example.animeapp.db.AnimeRecommendationsDatabase
import com.example.animeapp.repository.AnimeRecommendationsRepository
import com.example.animeapp.ui.animerecommendations.AnimeRecommendationsViewModel
import com.example.animeapp.ui.animerecommendations.AnimeRecommendationsViewModelProviderFactory

class MainActivity : AppCompatActivity() {
    lateinit var viewModel: AnimeRecommendationsViewModel
    private lateinit var binding: ActivityMainBinding
    val animeRecommendationsViewModel: AnimeRecommendationsViewModel
        get() = viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val animeRecommendationsRepository = AnimeRecommendationsRepository(AnimeRecommendationsDatabase(this))
        val viewModelProviderFactory = AnimeRecommendationsViewModelProviderFactory(animeRecommendationsRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(AnimeRecommendationsViewModel::class.java)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.animeRecommendationsFragment, R.id.aboutFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}