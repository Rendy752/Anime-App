package com.luminoverse.animevibe.ui.animeWatch

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.ui.animeWatch.components.InfoContentSection
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.PlayPauseLoadingButton
import com.luminoverse.animevibe.ui.animeWatch.videoPlayer.VideoPlayer
import com.luminoverse.animevibe.ui.common.ImageAspectRatio
import com.luminoverse.animevibe.ui.common.ImageDisplay
import com.luminoverse.animevibe.ui.common.ImageRoundedCorner
import com.luminoverse.animevibe.ui.main.MainActivity
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.ui.main.PlayerDisplayMode
import com.luminoverse.animevibe.ui.main.SnackbarMessage
import com.luminoverse.animevibe.ui.main.SnackbarMessageType
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.receivers.ScreenOffReceiver
import com.luminoverse.animevibe.utils.receivers.ScreenOnReceiver
import com.luminoverse.animevibe.utils.resource.Resource
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@SuppressLint("SourceLockedOrientationActivity", "ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeWatchScreen(
    modifier: Modifier = Modifier,
    malId: Int,
    episodeId: String,
    playerDisplayMode: PlayerDisplayMode,
    setPlayerDisplayMode: (PlayerDisplayMode) -> Unit,
    navController: NavHostController,
    networkDataSource: NetworkDataSource,
    mainState: MainState,
    showSnackbar: (SnackbarMessage) -> Unit,
    dismissSnackbar: () -> Unit,
    closePlayer: () -> Unit,
    watchState: WatchState,
    hlsPlayerCoreState: PlayerCoreState,
    hlsControlsStateFlow: StateFlow<ControlsState>,
    onAction: (WatchAction) -> Unit,
    dispatchPlayerAction: (HlsPlayerAction) -> Unit,
    getPlayer: () -> ExoPlayer?,
    captureScreenshot: suspend () -> String?,
    onEnterSystemPipMode: () -> Unit,
    rememberedTopPadding: Dp,
    screenHeightPx: Float,
    verticalDragOffset: Animatable<Float, *>,
    pipDragProgress: Float,
    maxVerticalDrag: Float,
    setMaxVerticalDrag: (Float) -> Unit,
    pipEndDestinationPx: Offset,
    pipEndSizePx: IntSize
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isScreenOn by remember { mutableStateOf(true) }
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
            scope.launch {
                verticalDragOffset.animateTo(maxVerticalDrag, spring(stiffness = 400f))
                setPlayerDisplayMode(PlayerDisplayMode.PIP)
                verticalDragOffset.snapTo(0f)
            }
        }
    }

    BackHandler(enabled = playerDisplayMode == PlayerDisplayMode.FULLSCREEN_LANDSCAPE || playerDisplayMode == PlayerDisplayMode.FULLSCREEN_PORTRAIT) {
        onBackPress()
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
            onAction(WatchAction.HandleSelectedEpisodeServer(episodeSourcesQuery, isRefresh = true))
        }
    }

    LaunchedEffect(
        watchState.animeDetail,
        watchState.animeDetailComplement,
        watchState.episodeDetailComplement,
        hlsPlayerCoreState.error
    ) {
        hlsPlayerCoreState.error?.let { errorMessage ->
            showSnackbar(
                SnackbarMessage(
                    message = errorMessage,
                    type = SnackbarMessageType.ERROR,
                    actionLabel = "RETRY",
                    onAction = { refreshEpisodeSources() }
                )
            )
        }
        if (watchState.animeDetail is Resource.Loading ||
            watchState.animeDetailComplement is Resource.Loading ||
            watchState.episodeDetailComplement is Resource.Loading ||
            hlsPlayerCoreState.error == null
        ) dismissSnackbar()
    }

    LaunchedEffect(mainState.networkStatus.isConnected) {
        if (!mainState.networkStatus.isConnected) return@LaunchedEffect
        if (getPlayer()?.isPlaying == false) dispatchPlayerAction(HlsPlayerAction.Play)
        if (watchState.episodeDetailComplement is Resource.Error && watchState.episodeSourcesQuery != null) {
            onAction(
                WatchAction.HandleSelectedEpisodeServer(
                    watchState.episodeSourcesQuery, isRefresh = true
                )
            )
        }
    }

    val player by remember { mutableStateOf(getPlayer()) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val isPortrait = !mainState.isLandscape && playerDisplayMode in listOf(
        PlayerDisplayMode.FULLSCREEN_LANDSCAPE, PlayerDisplayMode.FULLSCREEN_PORTRAIT
    )
    val isSideSheetVisible = !isPortrait && watchState.isSideSheetVisible && pipDragProgress == 0f

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerWidth = this.maxWidth
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .then(
                        if (playerDisplayMode == PlayerDisplayMode.PIP) Modifier.fillMaxSize()
                        else if (isPortrait) Modifier.height(screenWidth * 9 / 16)
                        else Modifier.fillMaxSize()
                    )
                    .weight(1f)
            ) {
                if (player == null || watchState.animeDetailComplement !is Resource.Success || watchState.episodeDetailComplement !is Resource.Success || watchState.episodeDetailComplement.data.sources.link.file.isEmpty() || watchState.animeDetailComplement.data.episodes == null || watchState.episodeSourcesQuery == null) {
                    ImageDisplay(
                        image = watchState.episodeDetailComplement.data?.screenshot,
                        imagePlaceholder = watchState.episodeDetailComplement.data?.imageUrl
                            ?: watchState.animeDetail.data?.images?.webp?.large_image_url,
                        ratio = ImageAspectRatio.WIDESCREEN.ratio,
                        contentDescription = "Anime cover",
                        roundedCorners = if (playerDisplayMode == PlayerDisplayMode.PIP) ImageRoundedCorner.ALL else ImageRoundedCorner.NONE
                    )
                } else {
                    player?.let { exoPlayer ->
                        VideoPlayer(
                            player = exoPlayer,
                            episodeDetailComplement = watchState.episodeDetailComplement.data,
                            episodeDetailComplements = watchState.episodeDetailComplements,
                            imagePlaceholder = watchState.animeDetail.data?.images?.webp?.large_image_url,
                            isRefreshing = watchState.animeDetail is Resource.Loading,
                            networkDataSource = networkDataSource,
                            coreState = hlsPlayerCoreState,
                            controlsStateFlow = hlsControlsStateFlow,
                            playerAction = dispatchPlayerAction,
                            isLandscape = mainState.isLandscape,
                            captureScreenshot = captureScreenshot,
                            updateStoredWatchState = { currentPosition, duration, screenShot ->
                                onAction(WatchAction.UpdateLastEpisodeWatchedId(watchState.episodeDetailComplement.data.id))
                                onAction(
                                    WatchAction.UpdateStoredWatchState(
                                        currentPosition, duration, screenShot
                                    )
                                )
                            },
                            isScreenOn = isScreenOn,
                            screenHeightPx = screenHeightPx,
                            isAutoPlayVideo = mainState.isAutoPlayVideo,
                            episodes = watchState.animeDetailComplement.data.episodes,
                            episodeSourcesQuery = watchState.episodeSourcesQuery,
                            handleSelectedEpisodeServer = { episodeSourcesQuery, isRefresh ->
                                onAction(
                                    WatchAction.HandleSelectedEpisodeServer(
                                        episodeSourcesQuery,
                                        isRefresh
                                    )
                                )
                            },
                            displayMode = playerDisplayMode,
                            setPlayerDisplayMode = setPlayerDisplayMode,
                            onEnterSystemPipMode = onEnterSystemPipMode,
                            isSideSheetVisible = watchState.isSideSheetVisible,
                            setSideSheetVisibility = {
                                onAction(
                                    WatchAction.SetSideSheetVisibility(it)
                                )
                            },
                            isAutoplayNextEpisodeEnabled = watchState.isAutoplayNextEpisodeEnabled,
                            setAutoplayNextEpisodeEnabled = {
                                onAction(
                                    WatchAction.SetAutoplayNextEpisodeEnabled(it)
                                )
                            },
                            rememberedTopPadding = rememberedTopPadding,
                            verticalDragOffset = verticalDragOffset,
                            maxVerticalDrag = maxVerticalDrag,
                            pipDragProgress = pipDragProgress,
                            pipEndDestinationPx = pipEndDestinationPx,
                            pipEndSizePx = pipEndSizePx,
                            onMaxDragAmountCalculated = { setMaxVerticalDrag(it) }
                        )
                    }
                }

                this@Row.AnimatedVisibility(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    visible = playerDisplayMode == PlayerDisplayMode.PIP,
                    enter = fadeIn(),
                    exit = fadeOut(animationSpec = tween(durationMillis = 0))
                ) {
                    PlayPauseLoadingButton(
                        playbackState = hlsPlayerCoreState.playbackState,
                        isRefreshing = watchState.animeDetail is Resource.Loading || watchState.animeDetailComplement is Resource.Loading || watchState.episodeDetailComplement is Resource.Loading,
                        isPlaying = hlsPlayerCoreState.isPlaying,
                        handleRestart = { dispatchPlayerAction(HlsPlayerAction.SeekTo(0)) },
                        handlePause = { dispatchPlayerAction(HlsPlayerAction.Pause) },
                        handlePlay = { dispatchPlayerAction(HlsPlayerAction.Play) },
                        size = 48.dp
                    )
                }

                this@Row.AnimatedVisibility(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    visible = playerDisplayMode == PlayerDisplayMode.PIP,
                    enter = fadeIn(),
                    exit = fadeOut(animationSpec = tween(durationMillis = 0))
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                onAction(WatchAction.SetInitialState(malId, episodeId))
                                dispatchPlayerAction(HlsPlayerAction.Reset)
                                closePlayer()
                            }
                            .background(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Custom Picture In Picture Player",
                            tint = Color.White,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            val sideSheetWidth by animateDpAsState(
                targetValue = if (isSideSheetVisible) containerWidth * 0.3f else 0.dp,
                label = "sideSheetWidthAnimation"
            )

            if (sideSheetWidth > 0.dp) {
                Column(modifier = Modifier.width(sideSheetWidth)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Info",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    onAction(
                                        WatchAction.SetSideSheetVisibility(false)
                                    )
                                }
                                .padding(8.dp),
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close currently watching anime info",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        InfoContentSection(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            animeDetail = watchState.animeDetail,
                            navController = navController,
                            setPlayerDisplayMode = setPlayerDisplayMode
                        )
                    }
                }
            }
        }
    }
}