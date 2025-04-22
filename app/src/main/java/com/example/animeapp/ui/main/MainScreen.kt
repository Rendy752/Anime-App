package com.example.animeapp.ui.main

import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.animeapp.R
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
import com.example.animeapp.ui.animeHome.AnimeHomeViewModel
import com.example.animeapp.ui.common_ui.MessageDisplay
import com.example.animeapp.ui.settings.SettingsScreen
import com.example.animeapp.utils.Navigation.navigateToAnimeDetail
import com.google.gson.Gson
import java.net.URLDecoder

@Composable
fun MainScreen(
    navController: NavHostController,
    mainState: MainState,
    mainAction: (MainAction) -> Unit
) {
    val activity = LocalActivity.current
    val gson = Gson()
    var isCurrentBottomScreen by remember { mutableStateOf(true) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomRoutes = BottomScreen.entries.map { it.route }
    isCurrentBottomScreen = BottomScreen.entries.any { it.route == currentRoute }

    fun getBottomBarEnterTransition(
        initialState: NavBackStackEntry,
        targetState: NavBackStackEntry
    ): EnterTransition {
        val initialRoute = initialState.destination.route
        val targetRoute = targetState.destination.route

        if (initialRoute in bottomRoutes && targetRoute in bottomRoutes) {
            val initialIndex = BottomScreen.orderedList.indexOfFirst { it.route == initialRoute }
            val targetIndex = BottomScreen.orderedList.indexOfFirst { it.route == targetRoute }

            val slideOffset: (Int) -> Int = when {
                targetIndex > initialIndex -> { fullWidth: Int -> fullWidth }
                targetIndex < initialIndex -> { fullWidth: Int -> -fullWidth }
                else -> { _: Int -> 0 }
            }
            return slideInHorizontally(animationSpec = tween(700), initialOffsetX = slideOffset)
        } else {
            return scaleIn(animationSpec = tween(700))
        }
    }

    fun getBottomBarExitTransition(
        initialState: NavBackStackEntry,
        targetState: NavBackStackEntry
    ): ExitTransition {
        val initialRoute = initialState.destination.route
        val targetRoute = targetState.destination.route

        if (initialRoute in bottomRoutes && targetRoute in bottomRoutes) {
            val initialIndex = BottomScreen.orderedList.indexOfFirst { it.route == initialRoute }
            val targetIndex = BottomScreen.orderedList.indexOfFirst { it.route == targetRoute }

            val slideOffset: (Int) -> Int = when {
                targetIndex > initialIndex -> { fullWidth: Int -> -fullWidth }
                targetIndex < initialIndex -> { fullWidth: Int -> fullWidth }
                else -> { _: Int -> 0 }
            }
            return slideOutHorizontally(animationSpec = tween(700), targetOffsetX = slideOffset)
        } else {
            return scaleOut(animationSpec = tween(700))
        }
    }

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
                            navController.navigateToAnimeDetail(animeId)
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
                enterTransition = { getBottomBarEnterTransition(initialState, targetState) },
                exitTransition = { getBottomBarExitTransition(initialState, targetState) },
                popEnterTransition = { getBottomBarEnterTransition(initialState, targetState) },
                popExitTransition = { getBottomBarExitTransition(initialState, targetState) }
            ) {
                composable(BottomScreen.Home.route) {
                    val animeHomeViewModel: AnimeHomeViewModel = hiltViewModel()
                    AnimeHomeScreen(
                        state = animeHomeViewModel.state.collectAsStateWithLifecycle().value,
                        mainState = mainState,
                        action = animeHomeViewModel::dispatch,
                        currentRoute = currentRoute,
                        navController = navController,
                    )
                }

                composable(BottomScreen.Recommendations.route) {
                    AnimeRecommendationsScreen(
                        navController = navController,
                        mainState = mainState
                    )
                }

                composable(BottomScreen.Search.route) {
                    AnimeSearchScreen(
                        navController = navController,
                        mainState = mainState
                    )
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
                        navController = navController,
                        mainState = mainState,
                        genre = genre,
                        producer = producer
                    )
                }

                composable(BottomScreen.Settings.route) {
                    SettingsScreen(
                        mainState = mainState,
                        mainAction = mainAction
                    )
                }

                composable(
                    "animeDetail/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("id") ?: 0
                    AnimeDetailScreen(
                        id = id,
                        navController = navController,
                        mainState = mainState
                    )
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
                        mainState = mainState,
                        isPipMode = isPipMode,
                        onEnterPipMode = {
                            if (mainState.isConnected) activity?.enterPictureInPictureMode(
                                PictureInPictureParams.Builder().build()
                            )
                        }
                    )
                }
            }
            if (isCurrentBottomScreen) {
                BottomNavigationBar(navController)
            }
            AnimatedVisibility(
                visible = !mainState.isConnected,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 1000, easing = EaseInOut)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 1000, easing = EaseInOut)
                )
            ) {
                MessageDisplay(
                    message = stringResource(R.string.no_internet_connection),
                    isRounded = false
                )
            }
        }
    }
}