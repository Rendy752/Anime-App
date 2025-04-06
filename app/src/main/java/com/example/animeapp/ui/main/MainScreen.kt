package com.example.animeapp.ui.main

import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.CommonIdentity
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.Genre
import com.example.animeapp.models.Producer
import com.example.animeapp.ui.animeDetail.AnimeDetailScreen
import com.example.animeapp.ui.animeRecommendations.AnimeRecommendationsScreen
import com.example.animeapp.ui.animeSearch.AnimeSearchScreen
import com.example.animeapp.ui.animeWatch.AnimeWatchScreen
import com.example.animeapp.ui.animeHome.AnimeHomeScreen
import com.example.animeapp.ui.settings.SettingsScreen
import com.google.gson.Gson
import java.net.URLDecoder

@Composable
fun MainScreen(navController: NavHostController) {
    val activity = LocalActivity.current
    val gson = Gson()
    var isBottomBarVisible by remember { mutableStateOf(true) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    isBottomBarVisible = BottomScreen.entries.any { it.route == currentRoute }

    LaunchedEffect(Unit) {
        activity?.let { activity ->
            val intent = activity.intent

            if (intent.action == Intent.ACTION_VIEW &&
                intent.scheme == "animeapp" &&
                intent.data != null
            ) {
                intent.data?.pathSegments?.let { segments ->
                    if (segments.size >= 2 && segments[0] == "detail") {
                        val animeId = segments[1].toIntOrNull()
                        if (animeId != null) {
                            navController.navigate("animeDetail/Title/$animeId")
                        }
                    } else {
                        Toast.makeText(activity, "Invalid URL", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
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
                startDestination = BottomScreen.Home.route,
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
                composable(BottomScreen.Home.route) {
                    AnimeHomeScreen(currentRoute, navController)
                }

                composable(BottomScreen.Recommendations.route) {
                    AnimeRecommendationsScreen(navController)
                }

                composable(BottomScreen.Search.route) {
                    AnimeSearchScreen(navController)
                }

                composable(
                    "${BottomScreen.Search.route}/{genreIdentity}/{producerIdentity}",
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

                composable(BottomScreen.Settings.route) {
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
                    "animeWatch/{animeDetailJson}/{animeDetailComplementJson}/{episodeIdEncoded}/{episodesJson}/{defaultEpisodeJson}",
                    arguments = listOf(
                        navArgument("animeDetailJson") { type = NavType.StringType },
                        navArgument("animeDetailComplementJson") { type = NavType.StringType },
                        navArgument("episodeIdEncoded") { type = NavType.StringType },
                        navArgument("episodesJson") { type = NavType.StringType },
                        navArgument("defaultEpisodeJson") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val animeDetailJson =
                        backStackEntry.arguments?.getString("animeDetailJson") ?: ""
                    val animeDetailComplementJson =
                        backStackEntry.arguments?.getString("animeDetailComplementJson") ?: ""
                    val episodeIdEncoded =
                        backStackEntry.arguments?.getString("episodeIdEncoded") ?: ""
                    val episodesJson = backStackEntry.arguments?.getString("episodesJson") ?: ""
                    val defaultEpisodeJson =
                        backStackEntry.arguments?.getString("defaultEpisodeJson") ?: ""

                    val animeDetail = Gson().fromJson(
                        URLDecoder.decode(animeDetailJson, "UTF-8"),
                        AnimeDetail::class.java
                    )
                    val animeDetailComplement = Gson().fromJson(
                        URLDecoder.decode(animeDetailComplementJson, "UTF-8"),
                        AnimeDetailComplement::class.java
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

                    var isPipMode by remember { mutableStateOf(false) }
                    val activity = LocalActivity.current as? MainActivity

                    DisposableEffect(activity) {
                        val onPictureInPictureModeChangedCallback = { isInPipMode: Boolean ->
                            isPipMode = isInPipMode
                        }
                        if (activity != null) {
                            activity.addOnPictureInPictureModeChangedListener(
                                onPictureInPictureModeChangedCallback
                            )
                            onDispose {
                                activity.removeOnPictureInPictureModeChangedListener(
                                    onPictureInPictureModeChangedCallback
                                )
                            }
                        }
                        onDispose {
                            activity?.removeOnPictureInPictureModeChangedListener(
                                onPictureInPictureModeChangedCallback
                            )
                        }
                    }

                    AnimeWatchScreen(
                        animeDetail = animeDetail,
                        animeDetailComplement = animeDetailComplement,
                        episodeId = episodeId,
                        episodesList = episodes,
                        defaultEpisode = defaultEpisode,
                        navController = navController,
                        isPipMode = isPipMode,
                        onEnterPipMode = {
                            activity?.enterPictureInPictureMode(
                                PictureInPictureParams.Builder().build()
                            )
                        }
                    )
                }
            }
            if (isBottomBarVisible) {
                BottomNavigationBar(navController)
            }
        }
    }
}