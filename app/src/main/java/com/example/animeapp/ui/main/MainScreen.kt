package com.example.animeapp.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.animeapp.ui.animeRecommendations.ui.AnimeRecommendationsScreen
import com.example.animeapp.ui.animeSearch.ui.AnimeSearchScreen
import com.example.animeapp.ui.settings.ui.SettingsScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        NavHost(
            navController,
            startDestination = Screen.Recommendations.route,
            Modifier.padding(paddingValues)
        ) {
            composable(Screen.Recommendations.route) { AnimeRecommendationsScreen(navController) }
            composable(Screen.Search.route) { AnimeSearchScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}