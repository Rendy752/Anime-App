package com.luminoverse.animevibe.ui.main

import android.app.PictureInPictureParams
import android.content.Intent
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.luminoverse.animevibe.ui.animeDetail.AnimeDetailScreen
import com.luminoverse.animevibe.ui.animeDetail.AnimeDetailViewModel
import com.luminoverse.animevibe.ui.animeHome.AnimeHomeScreen
import com.luminoverse.animevibe.ui.animeHome.AnimeHomeViewModel
import com.luminoverse.animevibe.ui.animeRecommendations.AnimeRecommendationsScreen
import com.luminoverse.animevibe.ui.animeRecommendations.AnimeRecommendationsViewModel
import com.luminoverse.animevibe.ui.animeSearch.AnimeSearchScreen
import com.luminoverse.animevibe.ui.animeSearch.AnimeSearchViewModel
import com.luminoverse.animevibe.ui.animeWatch.AnimeWatchScreen
import com.luminoverse.animevibe.ui.animeWatch.AnimeWatchViewModel
import com.luminoverse.animevibe.ui.animeWatch.WatchAction
import com.luminoverse.animevibe.ui.common.MessageDisplay
import com.luminoverse.animevibe.ui.episodeHistory.EpisodeHistoryScreen
import com.luminoverse.animevibe.ui.episodeHistory.EpisodeHistoryViewModel
import com.luminoverse.animevibe.ui.main.navigation.BottomNavigationBar
import com.luminoverse.animevibe.ui.main.navigation.getBottomBarEnterTransition
import com.luminoverse.animevibe.ui.main.navigation.getBottomBarExitTransition
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo
import com.luminoverse.animevibe.ui.main.navigation.navigateToAdjacentRoute
import com.luminoverse.animevibe.ui.settings.SettingsScreen
import com.luminoverse.animevibe.ui.settings.SettingsViewModel
import com.luminoverse.animevibe.utils.FullscreenUtils
import com.luminoverse.animevibe.utils.media.PipUtil.buildPipActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    intentChannel: Channel<Intent>,
    resetIdleTimer: () -> Unit,
    mainState: MainState,
    mainAction: (MainAction) -> Unit,
) {
    val activity = LocalActivity.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by rememberUpdatedState(navBackStackEntry?.destination?.route)
    val isConnected by rememberUpdatedState(mainState.isConnected)
    val isRtl by rememberUpdatedState(mainState.isRtl)
    val isCurrentBottomScreen by remember(currentRoute) {
        derivedStateOf { NavRoute.bottomRoutes.any { it.route == currentRoute } }
    }
    val coroutineScope = rememberCoroutineScope()
    var isNavigating by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) {
        resetIdleTimer()
        if (isCurrentBottomScreen) {
            activity?.window?.let { window ->
                FullscreenUtils.handleFullscreenToggle(
                    window = window,
                    isFullscreen = true,
                    isLandscape = true,
                    activity = activity
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        intentChannel.receiveAsFlow()
            .distinctUntilChanged { old, new -> old.data == new.data && old.action == new.action }
            .collect { intent ->
                if (intent.action != Intent.ACTION_VIEW || intent.data == null) return@collect
                intent.data?.let { uri ->
                    if (uri.scheme == "animevibe" && uri.host == "anime") {
                        val segments = uri.pathSegments
                        when {
                            segments.size >= 2 && segments[0] == "detail" -> {
                                val animeId = segments[1].toIntOrNull()
                                if (animeId != null) {
                                    navController.navigateTo(NavRoute.AnimeDetail.fromId(animeId))
                                } else {
                                    Toast.makeText(activity, "Invalid anime ID", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }

                            segments.size >= 3 && segments[0] == "watch" -> {
                                val malId = segments[1].toIntOrNull()
                                val episodeId = segments[2]
                                if (malId != null && episodeId.isNotEmpty()) {
                                    navController.popBackStack(
                                        navController.graph.startDestinationId,
                                        inclusive = false
                                    )
                                    navController.navigateTo(
                                        NavRoute.AnimeWatch.fromParams(
                                            malId,
                                            episodeId
                                        )
                                    )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .then(if (isCurrentBottomScreen) Modifier.navigationBarsPadding() else Modifier)
    ) {
        NavHost(
            navController = navController,
            startDestination = NavRoute.Home.route,
            modifier = Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (abs(dragAmount) > 100f && !isNavigating) {
                            isNavigating = true
                            coroutineScope.launch(Dispatchers.Main) {
                                val isNextLogical = if (isRtl) dragAmount > 0 else dragAmount < 0
                                navigateToAdjacentRoute(isNextLogical, currentRoute, navController)
                                isNavigating = false
                            }
                        }
                    }
                },
            enterTransition = { getBottomBarEnterTransition(initialState, targetState, isRtl) },
            exitTransition = { getBottomBarExitTransition(initialState, targetState, isRtl) },
            popEnterTransition = { getBottomBarEnterTransition(initialState, targetState, isRtl) },
            popExitTransition = { getBottomBarExitTransition(initialState, targetState, isRtl) }
        ) {
            composable(NavRoute.Home.route) {
                val viewModel: AnimeHomeViewModel = hiltViewModel()
                val homeState by viewModel.homeState.collectAsStateWithLifecycle()
                val carouselState by viewModel.carouselState.collectAsStateWithLifecycle()
                val remainingTimes by viewModel.remainingTimes.collectAsStateWithLifecycle()
                AnimeHomeScreen(
                    homeState = homeState,
                    carouselState = carouselState,
                    remainingTimes = remainingTimes,
                    onAction = viewModel::onAction,
                    mainState = mainState,
                    currentRoute = currentRoute,
                    navController = navController
                )
            }
            composable(NavRoute.Recommendations.route) {
                val viewModel: AnimeRecommendationsViewModel = hiltViewModel()
                val recommendationsState by viewModel.recommendationsState.collectAsStateWithLifecycle()
                AnimeRecommendationsScreen(
                    navController = navController,
                    mainState = mainState,
                    recommendationsState = recommendationsState,
                    onAction = viewModel::onAction
                )
            }
            composable(NavRoute.Search.route) {
                val viewModel: AnimeSearchViewModel = hiltViewModel()
                val searchState by viewModel.searchState.collectAsStateWithLifecycle()
                val filterSelectionState by viewModel.filterSelectionState.collectAsStateWithLifecycle()
                AnimeSearchScreen(
                    navController = navController,
                    mainState = mainState,
                    searchState = searchState,
                    filterSelectionState = filterSelectionState,
                    onAction = viewModel::onAction
                )
            }
            composable(
                route = NavRoute.SearchWithFilter.ROUTE_PATTERN,
                arguments = NavRoute.SearchWithFilter().arguments
            ) { backStackEntry ->
                val viewModel: AnimeSearchViewModel = hiltViewModel()
                val searchState by viewModel.searchState.collectAsStateWithLifecycle()
                val filterSelectionState by viewModel.filterSelectionState.collectAsStateWithLifecycle()
                val genreId = backStackEntry.arguments?.getString("genreId")?.toIntOrNull()
                val producerId = backStackEntry.arguments?.getString("producerId")?.toIntOrNull()
                AnimeSearchScreen(
                    navController = navController,
                    mainState = mainState,
                    genreId = genreId,
                    producerId = producerId,
                    searchState = searchState,
                    filterSelectionState = filterSelectionState,
                    onAction = viewModel::onAction
                )
            }
            composable(NavRoute.History.route) {
                val viewModel: EpisodeHistoryViewModel = hiltViewModel()
                val historyState by viewModel.historyState.collectAsStateWithLifecycle()
                EpisodeHistoryScreen(
                    currentRoute = currentRoute,
                    navController = navController,
                    mainState = mainState,
                    historyState = historyState,
                    onAction = viewModel::onAction
                )
            }
            composable(NavRoute.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                val settingsState by viewModel.state.collectAsStateWithLifecycle()
                SettingsScreen(
                    mainState = mainState,
                    mainAction = mainAction,
                    state = settingsState,
                    action = viewModel::onAction,
                )
            }
            composable(
                route = NavRoute.AnimeDetail.ROUTE_PATTERN,
                arguments = NavRoute.AnimeDetail(0).arguments
            ) { backStackEntry ->
                val viewModel: AnimeDetailViewModel = hiltViewModel()
                val detailState by viewModel.detailState.collectAsStateWithLifecycle()
                val episodeFilterState by viewModel.episodeFilterState.collectAsStateWithLifecycle()
                AnimeDetailScreen(
                    id = backStackEntry.arguments?.getInt("id") ?: 0,
                    navController = navController,
                    mainState = mainState,
                    detailState = detailState,
                    episodeFilterState = episodeFilterState,
                    onAction = viewModel::onAction
                )
            }
            composable(
                route = NavRoute.AnimeWatch.ROUTE_PATTERN,
                arguments = NavRoute.AnimeWatch(0, "").arguments
            ) { backStackEntry ->
                val viewModel: AnimeWatchViewModel =
                    hiltViewModel(LocalActivity.current as ViewModelStoreOwner)
                val watchState by viewModel.watchState.collectAsStateWithLifecycle()
                val playerUiState by viewModel.playerUiState.collectAsStateWithLifecycle()
                val playerCoreState by viewModel.playerCoreState.collectAsStateWithLifecycle()

                val activity = LocalActivity.current as? MainActivity

                DisposableEffect(activity) {
                    val onPictureInPictureModeChangedCallback: (Boolean) -> Unit = { isInPipMode ->
                        viewModel.onAction(WatchAction.SetPipMode(isInPipMode))
                        Log.d("MainScreen", "PiP mode changed: isInPipMode=$isInPipMode")
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

                val isPlaying by remember { derivedStateOf { playerCoreState.isPlaying } }
                LaunchedEffect(isPlaying) {
                    activity?.window?.let { window ->
                        if (isPlaying && !playerUiState.isPipMode) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                    if (playerUiState.isPipMode && activity != null) {
                        Log.d(
                            "MainScreen",
                            "Updating PiP params: isPlaying=${playerCoreState.isPlaying}"
                        )
                        val actions = buildPipActions(activity, playerCoreState.isPlaying)
                        activity.setPictureInPictureParams(
                            PictureInPictureParams.Builder()
                                .setActions(actions)
                                .build()
                        )
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        activity?.setPictureInPictureParams(
                            PictureInPictureParams.Builder().build()
                        )
                        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                AnimeWatchScreen(
                    malId = backStackEntry.arguments?.getInt("malId") ?: 0,
                    episodeId = backStackEntry.arguments?.getString("episodeId") ?: "",
                    navController = navController,
                    mainState = mainState,
                    watchState = watchState,
                    playerUiState = playerUiState,
                    hlsPlayerCoreState = playerCoreState,
                    hlsControlsState = viewModel.controlsState,
                    hlsPositionState = viewModel.positionState,
                    onAction = viewModel::onAction,
                    dispatchPlayerAction = viewModel::dispatchPlayerAction,
                    getPlayer = viewModel::getPlayer,
                    onEnterPipMode = {
                        if (isConnected && activity != null) {
                            Log.d(
                                "MainScreen",
                                "Entering PiP: isPlaying=${playerCoreState.isPlaying}"
                            )
                            val actions =
                                buildPipActions(activity, playerCoreState.isPlaying)
                            activity.enterPictureInPictureMode(
                                PictureInPictureParams.Builder()
                                    .setActions(actions)
                                    .build()
                            )
                            viewModel.onAction(WatchAction.SetPipMode(true))
                        }
                    }
                )
            }
        }
        AnimatedVisibility(
            visible = isCurrentBottomScreen && !mainState.isLandscape,
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
            modifier = Modifier.fillMaxWidth(),
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
                message = "No internet connection",
                isRounded = false
            )
        }
    }
}
