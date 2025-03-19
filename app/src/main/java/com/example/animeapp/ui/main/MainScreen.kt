package com.example.animeapp.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.animeapp.ui.animeDetail.ui.AnimeDetailScreen
import com.example.animeapp.ui.animeRecommendations.ui.AnimeRecommendationsScreen
import com.example.animeapp.ui.animeSearch.ui.AnimeSearchScreen
import com.example.animeapp.ui.animeWatch.ui.AnimeWatchScreen
import com.example.animeapp.ui.settings.ui.SettingsScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Surface(
        color = MaterialTheme.colorScheme.surface,
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
                composable(
                    "animeDetail/{animeTitle}/{animeId}",
                    arguments = listOf(navArgument("animeId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val animeTitle = backStackEntry.arguments?.getString("animeTitle") ?: ""
                    val animeId = backStackEntry.arguments?.getInt("animeId") ?: 0
                    AnimeDetailScreen(animeTitle, animeId, navController)
                }
                composable(
                    "animeWatch/{animeId}/{episodeId}/{defaultEpisodeId}",
                    arguments = listOf(
                        navArgument("animeId") { type = NavType.IntType },
                        navArgument("episodeId") { type = NavType.StringType },
                        navArgument("defaultEpisodeId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val animeId = backStackEntry.arguments?.getInt("animeId") ?: 0
                    val episodeId = backStackEntry.arguments?.getString("episodeId") ?: ""
                    val defaultEpisodeId = backStackEntry.arguments?.getString("defaultEpisodeId") ?: ""
                    AnimeWatchScreen(animeId, episodeId, defaultEpisodeId)
                }
            }
            BottomNavigationBar(navController)
        }
    }
}