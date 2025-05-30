package com.luminoverse.animevibe.ui.animeWatch

import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.utils.FullscreenUtils
import com.luminoverse.animevibe.utils.Resource
import com.luminoverse.animevibe.utils.ScreenOffReceiver
import com.luminoverse.animevibe.utils.ScreenOnReceiver
import com.luminoverse.animevibe.ui.animeWatch.components.AnimeWatchTopBar
import com.luminoverse.animevibe.ui.animeWatch.components.AnimeWatchContent
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.luminoverse.animevibe.ui.main.MainActivity
import com.luminoverse.animevibe.utils.HlsPlayerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeWatchScreen(
    malId: Int = 0,
    episodeId: String = "",
    navController: NavHostController,
    mainState: MainState,
    watchState: WatchState,
    playerUiState: PlayerUiState,
    hlsPlayerState: HlsPlayerState,
    onAction: (WatchAction) -> Unit,
    onEnterPipMode: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val state = rememberPullToRefreshState()

    var isScreenOn by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val activity = context as? MainActivity
    val screenOffReceiver = remember {
        ScreenOffReceiver {
            isScreenOn = false
            activity?.exitPipModeIfActive()
        }
    }
    val screenOnReceiver = remember { ScreenOnReceiver { isScreenOn = true } }

    val onBackPress: () -> Unit = {
        if (playerUiState.isFullscreen) {
            (context as? FragmentActivity)?.let { activity ->
                activity.window?.let { window ->
                    FullscreenUtils.handleFullscreenToggle(
                        window = window,
                        isFullscreen = true,
                        isLandscape = mainState.isLandscape,
                        activity = activity,
                        onFullscreenChange = { onAction(WatchAction.SetFullscreen(it)) }
                    )
                }
            }
        } else {
            navController.popBackStack()
        }
    }

    BackHandler {
        onBackPress()
    }

    LaunchedEffect(Unit) {
        scope.launch {
            onAction(WatchAction.SetInitialState(malId, episodeId))
        }
    }

    LaunchedEffect(watchState.episodeDetailComplement) {
        if (watchState.episodeDetailComplement is Resource.Error) {
            snackbarHostState.showSnackbar(
                "Failed to fetch episode sources, returning to the previous episode. Check your internet connection or try again later after 1 hour."
            )
            onAction(WatchAction.HandleSelectedEpisodeServer(watchState.episodeSourcesQuery))
        }
    }

    DisposableEffect(Unit) {
        context.registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        context.registerReceiver(screenOnReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))

        onDispose {
            context.unregisterReceiver(screenOffReceiver)
            context.unregisterReceiver(screenOnReceiver)
        }
    }

    LaunchedEffect(watchState.errorMessage) {
        watchState.errorMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!playerUiState.isPipMode && !playerUiState.isFullscreen) AnimeWatchTopBar(
                watchState = watchState,
                mainState = mainState,
                onContentIndexChange = { onAction(WatchAction.SetSelectedContentIndex(it)) },
                onHandleBackPress = onBackPress,
                onFavoriteToggle = { updatedComplement ->
                    onAction(WatchAction.SetFavorite(updatedComplement.isFavorite))
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = watchState.isRefreshing,
            onRefresh = {
                onAction(
                    WatchAction.HandleSelectedEpisodeServer(
                        watchState.episodeSourcesQuery,
                        isRefresh = true
                    )
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = state,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    isRefreshing = watchState.isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primary,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = state
                )
            }
        ) {
            val videoSize = if (mainState.isLandscape) Modifier.fillMaxSize()
            else if (!playerUiState.isPipMode && !playerUiState.isFullscreen) Modifier.height(250.dp)
            else Modifier.fillMaxSize()

            Column(modifier = Modifier.fillMaxSize()) {
                val videoPlayerModifier = Modifier
                    .then(if (mainState.isLandscape) Modifier.weight(0.5f) else Modifier.fillMaxWidth())
                    .then(videoSize)
                AnimeWatchContent(
                    navController = navController,
                    watchState = watchState,
                    isScreenOn = isScreenOn,
                    playerUiState = playerUiState,
                    hlsPlayerState = hlsPlayerState,
                    mainState = mainState,
                    updateStoredWatchState = { position, duration, screenshot ->
                        val updatedComplement =
                            (watchState.episodeDetailComplement as? Resource.Success)?.data?.copy(
                                isFavorite = watchState.isFavorite,
                                lastTimestamp = position,
                                duration = duration,
                                screenshot = screenshot,
                                lastWatched = LocalDateTime.now()
                                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            )
                        updatedComplement?.let {
                            onAction(WatchAction.UpdateEpisodeDetailComplement(it))
                            onAction(WatchAction.UpdateLastEpisodeWatchedId(it.id))
                        }
                    },
                    onLoadEpisodeDetailComplement = {
                        onAction(WatchAction.LoadEpisodeDetailComplement(it))
                    },
                    scrollState = scrollState,
                    onEnterPipMode = onEnterPipMode,
                    onFullscreenChange = { onAction(WatchAction.SetFullscreen(it)) },
                    onPlayerError = { onAction(WatchAction.SetErrorMessage(it)) },
                    handleSelectedEpisodeServer = { episodeSourcesQuery, isRefresh ->
                        onAction(
                            WatchAction.HandleSelectedEpisodeServer(
                                episodeSourcesQuery = episodeSourcesQuery,
                                isRefresh = isRefresh
                            )
                        )
                    },
                    modifier = videoPlayerModifier,
                    videoSize = videoSize
                )
            }
        }
    }
}