package com.luminoverse.animevibe.ui.animeWatch

import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.luminoverse.animevibe.utils.receivers.ScreenOffReceiver
import com.luminoverse.animevibe.utils.receivers.ScreenOnReceiver
import com.luminoverse.animevibe.ui.animeWatch.components.AnimeWatchContent
import kotlinx.coroutines.launch
import com.luminoverse.animevibe.ui.main.MainActivity
import androidx.compose.foundation.layout.padding
import androidx.media3.exoplayer.ExoPlayer
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.media.PositionState
import com.luminoverse.animevibe.utils.resource.Resource
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeWatchScreen(
    malId: Int = 0,
    episodeId: String = "",
    navController: NavHostController,
    mainState: MainState,
    watchState: WatchState,
    playerUiState: PlayerUiState,
    hlsPlayerCoreState: PlayerCoreState,
    hlsControlsState: StateFlow<ControlsState>,
    hlsPositionState: StateFlow<PositionState>,
    onAction: (WatchAction) -> Unit,
    dispatchPlayerAction: (HlsPlayerAction) -> Unit,
    getPlayer: () -> ExoPlayer?,
    onEnterPipMode: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()

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
                        setFullscreenChange = { onAction(WatchAction.SetFullscreen(it)) }
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

    LaunchedEffect(mainState.isConnected) {
        if (mainState.isConnected && watchState.episodeDetailComplement is Resource.Error && watchState.episodeSourcesQuery != null) {
            onAction(
                WatchAction.HandleSelectedEpisodeServer(
                    watchState.episodeSourcesQuery,
                    isRefresh = true
                )
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = watchState.isRefreshing,
            onRefresh = {
                watchState.episodeSourcesQuery?.let { episodeSourcesQuery ->
                    onAction(
                        WatchAction.HandleSelectedEpisodeServer(
                            episodeSourcesQuery,
                            isRefresh = true
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(if (playerUiState.isFullscreen) PaddingValues(0.dp) else paddingValues),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    isRefreshing = watchState.isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primary,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = pullToRefreshState
                )
            }
        ) {
            val videoSize =
                if (mainState.isLandscape || playerUiState.isFullscreen) Modifier.fillMaxSize()
                else if (!playerUiState.isPipMode) Modifier.height(250.dp)
                else Modifier.fillMaxSize()

            Column(modifier = Modifier.fillMaxSize()) {
                val videoPlayerModifier = Modifier
                    .then(
                        if (mainState.isLandscape && !playerUiState.isFullscreen) Modifier.weight(
                            0.5f
                        ) else Modifier.fillMaxWidth()
                    )
                    .then(videoSize)
                AnimeWatchContent(
                    navController = navController,
                    watchState = watchState,
                    isConnected = mainState.isConnected,
                    isScreenOn = isScreenOn,
                    isAutoPlayVideo = mainState.isAutoPlayVideo,
                    playerUiState = playerUiState,
                    mainState = mainState,
                    playerCoreState = hlsPlayerCoreState,
                    controlsState = hlsControlsState,
                    positionState = hlsPositionState,
                    dispatchPlayerAction = dispatchPlayerAction,
                    getPlayer = getPlayer,
                    onHandleBackPress = onBackPress,
                    onAction = onAction,
                    scrollState = scrollState,
                    onEnterPipMode = onEnterPipMode,
                    modifier = videoPlayerModifier,
                    videoSize = videoSize
                )
            }
        }
    }
}
