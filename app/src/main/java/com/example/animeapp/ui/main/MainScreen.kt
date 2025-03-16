package com.example.animeapp.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
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
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .navigationBarsPadding()
        ) {
            NavHost(
                navController = navController,
                startDestination = "recommendations",
                modifier = Modifier.weight(1f)
            ) {
                composable("recommendations") {
                    AnimeRecommendationsScreen(navController)
                }
                composable("search") {
                    AnimeSearchScreen(navController)
                }
                composable("settings") {
                    SettingsScreen()
                }
            }
            BottomNavigationBar(navController)
        }
    }
}