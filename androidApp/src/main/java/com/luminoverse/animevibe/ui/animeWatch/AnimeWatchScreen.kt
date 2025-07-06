package com.luminoverse.animevibe.ui.animeWatch

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.ui.animeWatch.components.AnimeWatchContent
import com.luminoverse.animevibe.ui.main.MainActivity
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.ui.main.PlayerDisplayMode
import com.luminoverse.animevibe.ui.main.SnackbarMessage
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.receivers.ScreenOffReceiver
import com.luminoverse.animevibe.utils.receivers.ScreenOnReceiver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("SourceLockedOrientationActivity", "ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeWatchScreen(
    malId: Int,
    episodeId: String,
    playerDisplayMode: PlayerDisplayMode,
    setPlayerDisplayMode: (PlayerDisplayMode) -> Unit,
    navController: NavHostController,
    networkDataSource: NetworkDataSource,
    mainState: MainState,
    showSnackbar: (SnackbarMessage) -> Unit,
    dismissSnackbar: () -> Unit,
    watchState: WatchState,
    snackbarFlow: Flow<SnackbarMessage>,
    hlsPlayerCoreState: PlayerCoreState,
    hlsControlsStateFlow: StateFlow<ControlsState>,
    onAction: (WatchAction) -> Unit,
    dispatchPlayerAction: (HlsPlayerAction) -> Unit,
    getPlayer: () -> ExoPlayer?,
    captureScreenshot: suspend () -> String?,
    onEnterSystemPipMode: () -> Unit,
    rememberedTopPadding: Dp,
    rememberedBottomPadding: Dp,
    pipEndDestinationPx: Offset,
    pipEndSizePx: IntSize
) {
    val scope = rememberCoroutineScope()
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
        if (playerDisplayMode == PlayerDisplayMode.FULLSCREEN_LANDSCAPE) {
            setPlayerDisplayMode(PlayerDisplayMode.FULLSCREEN_PORTRAIT)
        } else {
            setPlayerDisplayMode(PlayerDisplayMode.PIP)
        }
    }

    BackHandler(enabled = playerDisplayMode == PlayerDisplayMode.FULLSCREEN_LANDSCAPE || playerDisplayMode == PlayerDisplayMode.FULLSCREEN_PORTRAIT) {
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
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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

    LaunchedEffect(watchState.isRefreshing, hlsPlayerCoreState.error) {
        if (watchState.isRefreshing || hlsPlayerCoreState.error == null) dismissSnackbar()
    }

    LaunchedEffect(mainState.networkStatus.isConnected) {
        if (!mainState.networkStatus.isConnected) return@LaunchedEffect
        if (getPlayer()?.isPlaying == false) dispatchPlayerAction(HlsPlayerAction.Play)
        if (watchState.episodeDetailComplement == null && watchState.episodeSourcesQuery != null) {
            onAction(
                WatchAction.HandleSelectedEpisodeServer(
                    watchState.episodeSourcesQuery, isRefresh = true
                )
            )
        }
    }

    val verticalDragOffset = remember { Animatable(0f) }
    var maxVerticalDrag by remember { mutableFloatStateOf(Float.POSITIVE_INFINITY) }
    val screenHeightPx =
        with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val dismissDragThreshold = if (maxVerticalDrag.isFinite()) maxVerticalDrag else screenHeightPx
    val dragProgress = (verticalDragOffset.value / dismissDragThreshold).coerceIn(0f, 1f)

    PullToRefreshBox(
        isRefreshing = watchState.isRefreshing,
        onRefresh = { refreshEpisodeSources() },
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
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
        Box(modifier = Modifier.fillMaxSize()) {
            AnimeWatchContent(
                malId = malId,
                navController = navController,
                networkDataSource = networkDataSource,
                watchState = watchState,
                showSnackbar = showSnackbar,
                isScreenOn = isScreenOn,
                mainState = mainState,
                playerCoreState = hlsPlayerCoreState,
                controlsStateFlow = hlsControlsStateFlow,
                dispatchPlayerAction = dispatchPlayerAction,
                getPlayer = getPlayer,
                captureScreenshot = captureScreenshot,
                onHandleBackPress = onBackPress,
                onAction = onAction,
                playerDisplayMode = playerDisplayMode,
                setPlayerDisplayMode = setPlayerDisplayMode,
                onEnterSystemPipMode = onEnterSystemPipMode,
                dragProgress = dragProgress,
                maxVerticalDrag = maxVerticalDrag,
                setMaxVerticalDrag = { maxVerticalDrag = it },
                verticalDragOffset = verticalDragOffset,
                rememberedTopPadding = rememberedTopPadding,
                rememberedBottomPadding = rememberedBottomPadding,
                pipEndDestinationPx = pipEndDestinationPx,
                pipEndSizePx = pipEndSizePx
            )

            if (dragProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { }
                        )
                )
            }
        }
    }
}