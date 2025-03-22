package com.example.animeapp.ui.main

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.CommonIdentity
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.Genre
import com.example.animeapp.models.Producer
import com.example.animeapp.ui.animeDetail.ui.AnimeDetailScreen
import com.example.animeapp.ui.animeRecommendations.ui.AnimeRecommendationsScreen
import com.example.animeapp.ui.animeSearch.ui.AnimeSearchScreen
import com.example.animeapp.ui.animeWatch.ui.AnimeWatchScreen
import com.example.animeapp.ui.settings.ui.SettingsScreen
import com.google.gson.Gson
import java.net.URLDecoder

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val gson = Gson()
    var isBottomBarVisible by remember { mutableStateOf(true) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    isBottomBarVisible = when (currentRoute) {
        "recommendations",
        "search",
        "settings" -> true
        else -> false
    }

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
                modifier = Modifier.weight(1f),
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        tween(700)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        tween(700)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        tween(700)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        tween(700)
                    )
                }
            ) {
                composable("recommendations") {
                    AnimeRecommendationsScreen(navController)
                }

                composable("search") {
                    AnimeSearchScreen(navController)
                }

                composable(
                    "search/{genreIdentity}/{producerIdentity}",
                    arguments = listOf(
                        navArgument("genreIdentity") {
                            type = NavType.StringType
                            nullable = true
                        },
                        navArgument("producerIdentity") {
                            type = NavType.StringType
                            nullable = true
                        }
                    )
                ) { backStackEntry ->
                    val genreIdentityString = backStackEntry.arguments?.getString("genreIdentity")
                    val producerIdentityString =
                        backStackEntry.arguments?.getString("producerIdentity")

                    val genre: Genre? = genreIdentityString?.let {
                        if (it == "null") {
                            null
                        } else {
                            gson.fromJson(Uri.decode(it), CommonIdentity::class.java).mapToGenre()
                        }
                    }
                    val producer: Producer? = producerIdentityString?.let {
                        if (it == "null") {
                            null
                        } else {
                            gson.fromJson(Uri.decode(it), CommonIdentity::class.java)
                                .mapToProducer()
                        }
                    }

                    AnimeSearchScreen(
                        navController,
                        genre,
                        producer,
                    )
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
                    "animeWatch/{animeDetailJson}/{episodeIdEncoded}/{episodesJson}/{defaultEpisodeJson}",
                    arguments = listOf(
                        navArgument("animeDetailJson") { type = NavType.StringType },
                        navArgument("episodeIdEncoded") { type = NavType.StringType },
                        navArgument("episodesJson") { type = NavType.StringType },
                        navArgument("defaultEpisodeJson") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val animeDetailJson =
                        backStackEntry.arguments?.getString("animeDetailJson") ?: ""
                    val episodeIdEncoded =
                        backStackEntry.arguments?.getString("episodeIdEncoded") ?: ""
                    val episodesJson = backStackEntry.arguments?.getString("episodesJson") ?: ""
                    val defaultEpisodeJson =
                        backStackEntry.arguments?.getString("defaultEpisodeJson") ?: ""

                    val animeDetail = Gson().fromJson(
                        URLDecoder.decode(animeDetailJson, "UTF-8"),
                        AnimeDetail::class.java
                    )
                    val episodeId = URLDecoder.decode(episodeIdEncoded, "UTF-8")
                    val episodes = Gson().fromJson(
                        URLDecoder.decode(episodesJson, "UTF-8"),
                        Array<Episode>::class.java
                    ).toList()
                    val defaultEpisode = Gson().fromJson(
                        URLDecoder.decode(defaultEpisodeJson, "UTF-8"),
                        EpisodeDetailComplement::class.java
                    )

                    AnimeWatchScreen(
                        animeDetail,
                        episodeId,
                        episodes,
                        defaultEpisode,
                        navController
                    )
                }
            }
            if (isBottomBarVisible) {
                BottomNavigationBar(navController)
            }
        }
    }
}