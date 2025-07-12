package com.luminoverse.animevibe.ui.main

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.luminoverse.animevibe.ui.animeDetail.AnimeDetailScreen
import com.luminoverse.animevibe.ui.animeDetail.AnimeDetailViewModel
import com.luminoverse.animevibe.ui.animeHome.AnimeHomeScreen
import com.luminoverse.animevibe.ui.animeHome.AnimeHomeViewModel
import com.luminoverse.animevibe.ui.animeRecommendations.AnimeRecommendationsScreen
import com.luminoverse.animevibe.ui.animeRecommendations.AnimeRecommendationsViewModel
import com.luminoverse.animevibe.ui.animeSearch.AnimeSearchScreen
import com.luminoverse.animevibe.ui.animeSearch.AnimeSearchViewModel
import com.luminoverse.animevibe.ui.common.SharedImagePreviewer
import com.luminoverse.animevibe.ui.episodeHistory.EpisodeHistoryScreen
import com.luminoverse.animevibe.ui.episodeHistory.EpisodeHistoryViewModel
import com.luminoverse.animevibe.ui.main.navigation.getBottomBarEnterTransition
import com.luminoverse.animevibe.ui.main.navigation.getBottomBarExitTransition
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo
import com.luminoverse.animevibe.ui.settings.SettingsScreen
import com.luminoverse.animevibe.ui.settings.SettingsViewModel
import com.luminoverse.animevibe.utils.basicContainer
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    contentPadding: PaddingValues,
    navController: NavHostController,
    currentRoute: String?,
    intentChannel: Channel<Intent>,
    resetIdleTimer: () -> Unit,
    mainState: MainState,
    playerState: PlayerState?,
    mainAction: (MainAction) -> Unit,
    hlsPlayerUtils: HlsPlayerUtils
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current

    val isCurrentBottomScreen by remember(currentRoute) {
        derivedStateOf { NavRoute.bottomRoutes.any { it.route == currentRoute } }
    }

    var rememberedTopPadding by remember { mutableStateOf(0.dp) }
    val currentTopPadding = contentPadding.calculateTopPadding()
    if (currentTopPadding > 0.dp) {
        rememberedTopPadding = currentTopPadding
    }

    var rememberedBottomPadding by remember { mutableStateOf(0.dp) }
    val currentBottomPadding = contentPadding.calculateBottomPadding()
    if (currentBottomPadding > 0.dp) {
        rememberedBottomPadding = currentBottomPadding
    }

    LaunchedEffect(currentRoute, playerState?.displayMode) {
        resetIdleTimer()
        mainAction(MainAction.DismissSnackbar)
    }

    LaunchedEffect(Unit) {
        intentChannel.receiveAsFlow()
            .distinctUntilChanged { old, new -> old.data == new.data && old.action == new.action }
            .collect { intent ->
                val notificationId = intent.getIntExtra("notification_id", -1)
                if (notificationId != -1) {
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                }
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
                                    mainAction(
                                        MainAction.ShowSnackbar(
                                            SnackbarMessage(
                                                message = "Invalid anime ID",
                                                type = SnackbarMessageType.ERROR
                                            )
                                        )
                                    )
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
                                    mainAction.invoke(MainAction.PlayEpisode(malId, episodeId))
                                } else {
                                    mainAction(
                                        MainAction.ShowSnackbar(
                                            SnackbarMessage(
                                                message = "Invalid watch parameters",
                                                type = SnackbarMessageType.ERROR
                                            )
                                        )
                                    )
                                }
                            }

                            else -> {
                                mainAction(
                                    MainAction.ShowSnackbar(
                                        SnackbarMessage(
                                            message = "Invalid URL",
                                            type = SnackbarMessageType.ERROR
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
    }

    Box {
        Box(
            modifier = Modifier.padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = if (isCurrentBottomScreen || !mainState.isLandscape) rememberedBottomPadding else 0.dp
            )
        ) {
            Column {
                NavHost(
                    modifier = Modifier.weight(1f),
                    navController = navController,
                    startDestination = NavRoute.Home.route,
                    enterTransition = {
                        getBottomBarEnterTransition(initialState, targetState)
                    },
                    exitTransition = {
                        getBottomBarExitTransition(initialState, targetState)
                    },
                    popEnterTransition = {
                        getBottomBarEnterTransition(initialState, targetState)
                    },
                    popExitTransition = {
                        getBottomBarExitTransition(initialState, targetState)
                    }
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
                            navController = navController,
                            isVideoPlayerVisible = playerState != null,
                            playEpisode = { malId, episodeId ->
                                mainAction.invoke(MainAction.PlayEpisode(malId, episodeId))
                            },
                            rememberedTopPadding = rememberedTopPadding
                        )
                    }
                    composable(NavRoute.Recommendations.route) {
                        val viewModel: AnimeRecommendationsViewModel = hiltViewModel()
                        val recommendationsState by viewModel.recommendationsState.collectAsStateWithLifecycle()
                        AnimeRecommendationsScreen(
                            navController = navController,
                            rememberedTopPadding = rememberedTopPadding,
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
                            rememberedTopPadding = rememberedTopPadding,
                            mainState = mainState,
                            showImagePreview = { mainAction.invoke(MainAction.ShowImagePreview(it)) },
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
                        val producerId =
                            backStackEntry.arguments?.getString("producerId")?.toIntOrNull()
                        AnimeSearchScreen(
                            navController = navController,
                            rememberedTopPadding = rememberedTopPadding,
                            mainState = mainState,
                            showImagePreview = { mainAction.invoke(MainAction.ShowImagePreview(it)) },
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
                            playEpisode = { malId, episodeId ->
                                mainAction.invoke(MainAction.PlayEpisode(malId, episodeId))
                            },
                            rememberedTopPadding = rememberedTopPadding,
                            showSnackbar = { mainAction.invoke(MainAction.ShowSnackbar(it)) },
                            mainState = mainState,
                            showImagePreview = { mainAction.invoke(MainAction.ShowImagePreview(it)) },
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
                            settingsState = settingsState,
                            onSettingsAction = viewModel::onAction,
                            rememberedTopPadding = rememberedTopPadding
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
                            playEpisode = { malId, episodeId ->
                                mainAction.invoke(MainAction.PlayEpisode(malId, episodeId))
                            },
                            rememberedTopPadding = rememberedTopPadding,
                            mainState = mainState,
                            checkNotificationPermission = { mainAction.invoke(MainAction.CheckNotificationPermission) },
                            setPostNotificationsPermission = {
                                mainAction.invoke(
                                    MainAction.SetPostNotificationsPermission(
                                        it
                                    )
                                )
                            },
                            showSnackbar = { mainAction.invoke(MainAction.ShowSnackbar(it)) },
                            dismissSnackbar = { mainAction.invoke(MainAction.DismissSnackbar) },
                            showImagePreview = { mainAction.invoke(MainAction.ShowImagePreview(it)) },
                            detailState = detailState,
                            snackbarFlow = viewModel.snackbarFlow,
                            episodeFilterState = episodeFilterState,
                            onAction = viewModel::onAction
                        )
                    }
                }
                AnimatedVisibility(
                    modifier = Modifier.fillMaxWidth(),
                    visible = !mainState.networkStatus.isConnected,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    Text(
                        text = "No Internet Connection",
                        modifier = Modifier
                            .basicContainer(
                                isError = true,
                                useBorder = false,
                                roundedCornerShape = RoundedCornerShape(0.dp),
                                outerPadding = PaddingValues(0.dp),
                                innerPadding = PaddingValues(8.dp)
                            ),
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            AnimatedVisibility(
                visible = isCurrentBottomScreen && currentRoute != NavRoute.Home.route,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                Spacer(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f))
                        .fillMaxWidth()
                        .height(rememberedTopPadding)
                )
            }

            mainState.sharedImageState?.let { imageState ->
                SharedImagePreviewer(
                    sharedImageState = imageState,
                    onDismiss = {
                        mainAction(MainAction.DismissImagePreview)
                    }
                )
            }
        }

        AnimatedVisibility(
            modifier = Modifier.fillMaxWidth(),
            visible = playerState != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            playerState?.let { playerState ->
                PlayerHost(
                    playerState = playerState,
                    mainState = mainState,
                    onAction = mainAction,
                    hlsPlayerUtils = hlsPlayerUtils,
                    isCurrentBottomScreen = isCurrentBottomScreen,
                    rememberedTopPadding = rememberedTopPadding,
                    rememberedBottomPadding = rememberedBottomPadding,
                    startPadding = contentPadding.calculateStartPadding(layoutDirection),
                    endPadding = contentPadding.calculateEndPadding(layoutDirection),
                    navController = navController
                )
            }
        }
    }
}