package com.example.animeapp.ui.main

import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.animeapp.AnimeApplication
import com.example.animeapp.R
import com.example.animeapp.models.CommonIdentity
import com.example.animeapp.ui.animeDetail.AnimeDetailScreen
import com.example.animeapp.ui.animeRecommendations.AnimeRecommendationsScreen
import com.example.animeapp.ui.animeSearch.AnimeSearchScreen
import com.example.animeapp.ui.animeWatch.AnimeWatchScreen
import com.example.animeapp.ui.animeHome.AnimeHomeScreen
import com.example.animeapp.ui.animeHome.AnimeHomeViewModel
import com.example.animeapp.ui.common_ui.MessageDisplay
import com.example.animeapp.ui.settings.SettingsScreen
import com.example.animeapp.utils.Navigation.navigateToAnimeDetail
import com.example.animeapp.utils.Navigation.navigateToAnimeWatch
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    intentChannel: Channel<Intent>,
    onResetIdleTimer: () -> Unit,
    mainState: MainState,
    mainAction: (MainAction) -> Unit
) {
    val activity = LocalActivity.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by rememberUpdatedState(navBackStackEntry?.destination?.route)
    val isConnected by rememberUpdatedState(mainState.isConnected)

    val isCurrentBottomScreen by remember(currentRoute) {
        derivedStateOf { BottomScreen.entries.any { it.route == currentRoute } }
    }

    LaunchedEffect(Unit) {
        intentChannel.receiveAsFlow()
            .distinctUntilChanged { old, new -> old.data == new.data && old.action == new.action }
            .collect { intent ->
                if (intent.action != Intent.ACTION_VIEW || intent.data == null) return@collect
                intent.data?.let { uri ->
                    if (uri.scheme == "animeapp" && uri.host == "anime") {
                        val segments = uri.pathSegments
                        when {
                            segments.size >= 2 && segments[0] == "detail" -> {
                                val animeId = segments[1].toIntOrNull()
                                if (animeId != null) {
                                    navController.navigateToAnimeDetail(animeId)
                                } else {
                                    Toast.makeText(activity, "Invalid anime ID", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }

                            segments.size >= 3 && segments[0] == "watch" -> {
                                val malId = segments[1].toIntOrNull()
                                val episodeId = segments[2]
                                if (malId != null && episodeId.isNotEmpty()) {
                                    val currentArgs = navBackStackEntry?.arguments
                                    val currentMalId = currentArgs?.getInt("malId")
                                    val currentEpisodeId = currentArgs?.getString("episodeId")
                                    if (currentMalId != malId || currentEpisodeId != episodeId) {
                                        navController.popBackStack(
                                            "animeWatch/{malId}/{episodeId}",
                                            inclusive = true,
                                            saveState = false
                                        )
                                        navController.navigateToAnimeWatch(malId, episodeId)
                                    }
                                } else {
                                    Toast.makeText(
                                        activity,
                                        "Invalid watch parameters",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            else -> {
                                Toast.makeText(activity, "Invalid URL", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
    }

    LaunchedEffect(currentRoute) {
        onResetIdleTimer()
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
                    val homeState by animeHomeViewModel.state.collectAsStateWithLifecycle()
                    AnimeHomeScreen(
                        state = homeState,
                        mainState = mainState,
                        action = remember { animeHomeViewModel::dispatch },
                        currentRoute = currentRoute,
                        navController = navController
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
                ) {
                    val genreIdentityString = it.arguments?.getString("genreIdentity")
                    val producerIdentityString = it.arguments?.getString("producerIdentity")
                    val gson = Gson()
                    val genre = remember(genreIdentityString) {
                        genreIdentityString?.let {
                            if (it == "null") null
                            else gson.fromJson(Uri.decode(it), CommonIdentity::class.java)
                                .mapToGenre()
                        }
                    }
                    val producer = remember(producerIdentityString) {
                        producerIdentityString?.let {
                            if (it == "null") null
                            else gson.fromJson(Uri.decode(it), CommonIdentity::class.java)
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
                ) {
                    AnimeDetailScreen(
                        id = it.arguments?.getInt("id") ?: 0,
                        navController = navController,
                        mainState = mainState
                    )
                }
                composable(
                    "animeWatch/{malId}/{episodeId}",
                    arguments = listOf(
                        navArgument("malId") { type = NavType.IntType },
                        navArgument("episodeId") { type = NavType.StringType }
                    )
                ) {
                    var isPipMode by remember { mutableStateOf(false) }
                    val activity = LocalActivity.current as? MainActivity
                    val mediaService =
                        (activity?.application as? AnimeApplication)?.getMediaPlaybackService()
                    val isPlaying by mediaService?.isPlayingState?.collectAsStateWithLifecycle()
                        ?: remember { mutableStateOf(false) }

                    DisposableEffect(activity) {
                        val onPictureInPictureModeChangedCallback: (Boolean) -> Unit = { isInPipMode: Boolean ->
                            isPipMode = isInPipMode
                            Log.d("MainScreen", "PiP mode changed: isPipMode=$isInPipMode")
                            Unit
                        }
                        activity?.addOnPictureInPictureModeChangedListener(
                            onPictureInPictureModeChangedCallback
                        )
                        onDispose {
                            activity?.removeOnPictureInPictureModeChangedListener(
                                onPictureInPictureModeChangedCallback
                            )
                        }
                    }

                    LaunchedEffect(isPipMode, isPlaying) {
                        if (isPipMode && activity != null) {
                            Log.d("MainScreen", "Updating PiP params: isPlaying=$isPlaying")
                            val actions = MainActivity.buildPipActions(activity, isPlaying)
                            activity.setPictureInPictureParams(
                                PictureInPictureParams.Builder()
                                    .setActions(actions)
                                    .build()
                            )
                        }
                    }

                    AnimeWatchScreen(
                        malId = it.arguments?.getInt("malId") ?: 0,
                        episodeId = it.arguments?.getString("episodeId") ?: "",
                        navController = navController,
                        mainState = mainState,
                        isPipMode = isPipMode,
                        onEnterPipMode = {
                            if (isConnected && activity != null) {
                                val service =
                                    (activity.application as AnimeApplication).getMediaPlaybackService()
                                val isPlaying = service?.isPlayingState?.value == true
                                Log.d("MainScreen", "Entering PiP: isPlaying=$isPlaying")
                                val actions = MainActivity.buildPipActions(activity, isPlaying)
                                activity.enterPictureInPictureMode(
                                    PictureInPictureParams.Builder()
                                        .setActions(actions)
                                        .build()
                                )
                            }
                        }
                    )
                }
            }
            AnimatedVisibility(
                visible = isCurrentBottomScreen,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 700, easing = EaseInOut)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 700, easing = EaseInOut)
                )
            ) {
                BottomNavigationBar(navController = navController)
            }
            AnimatedVisibility(
                visible = !isConnected,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 700, easing = EaseInOut)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 700, easing = EaseInOut)
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