package com.luminoverse.animevibe.ui.animeWatch

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.utils.FullscreenUtils
import com.luminoverse.animevibe.utils.receivers.ScreenOffReceiver
import com.luminoverse.animevibe.utils.receivers.ScreenOnReceiver
import com.luminoverse.animevibe.ui.animeWatch.components.AnimeWatchContent
import kotlinx.coroutines.launch
import com.luminoverse.animevibe.ui.main.MainActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.media3.exoplayer.ExoPlayer
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.ui.main.SnackbarMessage
import com.luminoverse.animevibe.ui.main.SnackbarMessageType
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeWatchScreen(
    malId: Int,
    episodeId: String,
    navController: NavHostController,
    networkDataSource: NetworkDataSource,
    mainState: MainState,
    showSnackbar: (SnackbarMessage) -> Unit,
    dismissSnackbar: () -> Unit,
    watchState: WatchState,
    playerUiState: PlayerUiState,
    hlsPlayerCoreState: PlayerCoreState,
    hlsControlsStateFlow: StateFlow<ControlsState>,
    onAction: (WatchAction) -> Unit,
    dispatchPlayerAction: (HlsPlayerAction) -> Unit,
    getPlayer: () -> ExoPlayer?,
    captureScreenshot: suspend () -> String?,
    onEnterPipMode: () -> Unit
) {
    val density = LocalDensity.current
    val statusBarPadding = with(density) {
        WindowInsets.systemBars.getTop(density).toDp()
    }

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

    LaunchedEffect(mainState.isLandscape) {
        onAction(WatchAction.SetSideSheetVisibility(false))
        activity?.window?.let { window ->
            if (mainState.isLandscape) {
                FullscreenUtils.handleFullscreenToggle(
                    window = window,
                    isFullscreen = false,
                    setFullscreenChange = { onAction(WatchAction.SetFullscreen(it)) }
                )
            } else {
                FullscreenUtils.handleFullscreenToggle(
                    window = window,
                    isFullscreen = true,
                    setFullscreenChange = { onAction(WatchAction.SetFullscreen(it)) }
                )
            }
        }
    }

    val onBackPress: () -> Unit = {
        if (playerUiState.isFullscreen) {
            activity?.window?.let { window ->
                FullscreenUtils.handleFullscreenToggle(
                    window = window,
                    isFullscreen = true,
                    setFullscreenChange = { onAction(WatchAction.SetFullscreen(it)) }
                )
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
            (context as? Activity)?.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            context.unregisterReceiver(screenOffReceiver)
            context.unregisterReceiver(screenOnReceiver)
        }
    }

    fun refreshEpisodeSources() {
        watchState.episodeSourcesQuery?.let { episodeSourcesQuery ->
            onAction(
                WatchAction.HandleSelectedEpisodeServer(
                    episodeSourcesQuery,
                    isRefresh = true
                )
            )
        }
    }

    LaunchedEffect(watchState.errorMessage, watchState.isRefreshing, mainState.isConnected) {
        when {
            watchState.isRefreshing -> dismissSnackbar()
            watchState.errorMessage != null -> {
                showSnackbar(
                    SnackbarMessage(
                        message = watchState.errorMessage,
                        type = SnackbarMessageType.ERROR,
                        actionLabel = "RETRY",
                        onAction = { refreshEpisodeSources() }
                    )
                )
            }
            else -> dismissSnackbar()
        }
    }

    LaunchedEffect(mainState.isConnected) {
        if (mainState.isConnected && watchState.episodeDetailComplement == null && watchState.episodeSourcesQuery != null) {
            onAction(
                WatchAction.HandleSelectedEpisodeServer(
                    watchState.episodeSourcesQuery,
                    isRefresh = true
                )
            )
        }
    }

    PullToRefreshBox(
        isRefreshing = watchState.isRefreshing,
        onRefresh = { refreshEpisodeSources() },
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (mainState.isLandscape) 0.dp else statusBarPadding),
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
        Column(modifier = Modifier.fillMaxSize()) {
            val videoPlayerModifier = Modifier
                .fillMaxWidth()
                .then(if (mainState.isLandscape) Modifier.fillMaxSize() else Modifier.height(250.dp))
            AnimeWatchContent(
                malId = malId,
                navController = navController,
                networkDataSource = networkDataSource,
                watchState = watchState,
                isScreenOn = isScreenOn,
                isAutoPlayVideo = mainState.isAutoPlayVideo,
                playerUiState = playerUiState,
                mainState = mainState,
                playerCoreState = hlsPlayerCoreState,
                controlsStateFlow = hlsControlsStateFlow,
                dispatchPlayerAction = dispatchPlayerAction,
                getPlayer = getPlayer,
                captureScreenshot = captureScreenshot,
                onHandleBackPress = onBackPress,
                onAction = onAction,
                scrollState = scrollState,
                onEnterPipMode = onEnterPipMode,
                modifier = videoPlayerModifier,
            )
        }
    }
}