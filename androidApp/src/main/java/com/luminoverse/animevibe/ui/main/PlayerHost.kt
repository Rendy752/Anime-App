package com.luminoverse.animevibe.ui.main

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminoverse.animevibe.ui.animeWatch.AnimeWatchScreen
import com.luminoverse.animevibe.ui.animeWatch.AnimeWatchViewModel
import com.luminoverse.animevibe.ui.animeWatch.WatchAction
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.PlayPauseLoadingButton
import com.luminoverse.animevibe.utils.media.PipUtil.buildPipActions
import kotlin.math.roundToInt


@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun PlayerHost(
    playerState: PlayerState,
    mainState: MainState,
    onAction: (MainAction) -> Unit,
    hlsPlayerUtils: HlsPlayerUtils,
    rememberedTopPadding: Dp,
    rememberedBottomPadding: Dp,
    navController: NavHostController
) {
    val configuration = LocalConfiguration.current
    var pipContainerSize by remember { mutableStateOf(IntSize.Zero) }

    val pipModifier = Modifier
        .offset { playerState.pipOffset }
        .width(250.dp)
        .aspectRatio(16f / 9f)
        .shadow(8.dp, RoundedCornerShape(12.dp))
        .clip(RoundedCornerShape(12.dp))
        .onSizeChanged { pipContainerSize = it }
        .clickable {
            onAction(MainAction.SetPlayerDisplayMode(PlayerDisplayMode.FULLSCREEN))
        }
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                val screenWidthPx = configuration.screenWidthDp.dp.toPx()
                val screenHeightPx = configuration.screenHeightDp.dp.toPx()
                val currentOffset = playerState.pipOffset
                val newOffset = IntOffset(
                    (currentOffset.x + dragAmount.x).roundToInt(),
                    (currentOffset.y + dragAmount.y).roundToInt()
                )
                val constrainedX =
                    newOffset.x.toFloat().coerceIn(0f, screenWidthPx - pipContainerSize.width)
                val constrainedY =
                    newOffset.y.toFloat().coerceIn(0f, screenHeightPx - pipContainerSize.height)

                onAction(
                    MainAction.UpdatePlayerPipOffset(
                        IntOffset(
                            constrainedX.roundToInt(),
                            constrainedY.roundToInt()
                        )
                    )
                )
            }
        }

    val containerModifier = when (playerState.displayMode) {
        PlayerDisplayMode.FULLSCREEN -> Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                top = if (!mainState.isLandscape) rememberedTopPadding else 0.dp,
                bottom = if (!mainState.isLandscape) rememberedBottomPadding else 0.dp
            )

        PlayerDisplayMode.PIP -> pipModifier
    }

    Box(modifier = containerModifier) {
        val watchViewModel: AnimeWatchViewModel = hiltViewModel()
        val watchState by watchViewModel.watchState.collectAsStateWithLifecycle()
        val playerUiState by watchViewModel.playerUiState.collectAsStateWithLifecycle()
        val playerCoreState by watchViewModel.playerCoreState.collectAsStateWithLifecycle()

        LaunchedEffect(playerState.malId, playerState.episodeId) {
            watchViewModel.onAction(
                WatchAction.SetInitialState(
                    playerState.malId,
                    playerState.episodeId
                )
            )
        }

        val activity = LocalActivity.current as? MainActivity

        DisposableEffect(activity) {
            val onPictureInPictureModeChangedCallback: (Boolean) -> Unit =
                { isInPipMode ->
                    watchViewModel.onAction(WatchAction.SetPipMode(isInPipMode))
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
            malId = playerState.malId,
            episodeId = playerState.episodeId,
            displayMode = playerState.displayMode,
            navController = navController,
            networkDataSource = watchViewModel.networkDataSource,
            mainState = mainState,
            showSnackbar = { onAction(MainAction.ShowSnackbar(it)) },
            dismissSnackbar = { onAction(MainAction.DismissSnackbar) },
            watchState = watchState,
            playerUiState = playerUiState,
            snackbarFlow = watchViewModel.snackbarFlow,
            hlsPlayerCoreState = playerCoreState,
            hlsControlsStateFlow = watchViewModel.controlsState,
            onAction = watchViewModel::onAction,
            dispatchPlayerAction = watchViewModel::dispatchPlayerAction,
            getPlayer = watchViewModel::getPlayer,
            captureScreenshot = { watchViewModel.captureScreenshot() },
            onEnterPipMode = {
                onAction(MainAction.SetPlayerDisplayMode(PlayerDisplayMode.PIP))
            },
            onEnterSystemPipMode = {
                if (activity != null) {
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
                    watchViewModel.onAction(WatchAction.SetPipMode(true))
                }
            }
        )

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            visible = playerState.displayMode == PlayerDisplayMode.PIP,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayPauseLoadingButton(
                playbackErrorMessage = playerCoreState.error,
                playbackState = playerCoreState.playbackState,
                isPlaying = isPlaying,
                onSeekTo = { hlsPlayerUtils.dispatch(HlsPlayerAction.SeekTo(0)) },
                handlePause = { hlsPlayerUtils.dispatch(HlsPlayerAction.Pause) },
                handlePlay = { hlsPlayerUtils.dispatch(HlsPlayerAction.Play) },
                size = 48.dp
            )
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
            visible = playerState.displayMode == PlayerDisplayMode.PIP,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        onClick = {
                            hlsPlayerUtils.dispatch(HlsPlayerAction.Reset)
                            onAction(MainAction.ClosePlayer)
                        }
                    )
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
}