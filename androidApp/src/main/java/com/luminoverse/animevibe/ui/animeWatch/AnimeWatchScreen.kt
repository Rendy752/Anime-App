package com.luminoverse.animevibe.ui.animeWatch

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.media3.exoplayer.ExoPlayer
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.ui.main.SnackbarMessage
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeWatchScreen(
    malId: Int,
    episodeId: String,
    navController: NavHostController,
    rememberTopPadding: Dp,
    networkDataSource: NetworkDataSource,
    mainState: MainState,
    showSnackbar: (SnackbarMessage) -> Unit,
    dismissSnackbar: () -> Unit,
    watchState: WatchState,
    playerUiState: PlayerUiState,
    snackbarFlow: Flow<SnackbarMessage>,
    hlsPlayerCoreState: PlayerCoreState,
    hlsControlsStateFlow: StateFlow<ControlsState>,
    onAction: (WatchAction) -> Unit,
    dispatchPlayerAction: (HlsPlayerAction) -> Unit,
    getPlayer: () -> ExoPlayer?,
    captureScreenshot: suspend () -> String?,
    onEnterPipMode: () -> Unit
) {
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
        scope.launch {
            snackbarFlow.collectLatest { snackbarMessage ->
                showSnackbar(snackbarMessage)
            }
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

    LaunchedEffect(watchState.isRefreshing) {
        if (watchState.isRefreshing) dismissSnackbar()
    }

    LaunchedEffect(mainState.networkStatus.isConnected) {
        if (mainState.networkStatus.isConnected && watchState.episodeDetailComplement == null && watchState.episodeSourcesQuery != null) {
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
            .padding(top = if (mainState.isLandscape) 0.dp else rememberTopPadding),
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
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val videoHeight = screenWidth * 9 / 16
        Column(modifier = Modifier.fillMaxSize()) {
            val videoPlayerModifier = Modifier.fillMaxWidth()
                .then(if (mainState.isLandscape) Modifier.fillMaxSize() else Modifier.height(videoHeight))
            AnimeWatchContent(
                malId = malId,
                navController = navController,
                networkDataSource = networkDataSource,
                watchState = watchState,
                showSnackbar = showSnackbar,
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