package com.luminoverse.animevibe.ui.main

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.ui.animeWatch.AnimeWatchScreen
import com.luminoverse.animevibe.ui.animeWatch.AnimeWatchViewModel
import com.luminoverse.animevibe.ui.animeWatch.WatchAction
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.PlayPauseLoadingButton
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import com.luminoverse.animevibe.utils.media.PipUtil.buildPipActions
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.gestures.awaitEachGesture


@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun PlayerHost(
    playerState: PlayerState,
    mainState: MainState,
    onAction: (MainAction) -> Unit,
    hlsPlayerUtils: HlsPlayerUtils,
    isCurrentBottomScreen: Boolean,
    rememberedTopPadding: Dp,
    rememberedBottomPadding: Dp,
    startPadding: Dp,
    endPadding: Dp,
    navController: NavHostController
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = Modifier.fillMaxSize()) {
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current
            val scope = rememberCoroutineScope()

            val animatableRelativeOffset =
                remember { Animatable(playerState.pipRelativeOffset, Offset.VectorConverter) }

            LaunchedEffect(playerState.pipRelativeOffset) {
                if (animatableRelativeOffset.value != playerState.pipRelativeOffset) {
                    animatableRelativeOffset.snapTo(playerState.pipRelativeOffset)
                }
            }

            val topPaddingPx = with(density) { rememberedTopPadding.toPx() }
            val bottomPaddingPx = with(density) { rememberedBottomPadding.toPx() }
            val startPaddingPx = with(density) { startPadding.toPx() }
            val endPaddingPx = with(density) { endPadding.toPx() }

            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

            val animatedPipWidth by animateDpAsState(
                targetValue = playerState.pipWidth, animationSpec = spring(), label = "pipWidth"
            )
            val pipAspectRatio = 16f / 9f

            val pipWidthPx =
                remember(animatedPipWidth) { with(density) { animatedPipWidth.toPx() } }
            val pipHeightPx =
                remember(animatedPipWidth) { with(density) { (animatedPipWidth / pipAspectRatio).toPx() } }

            val minX = startPaddingPx
            val maxX = remember(pipWidthPx, screenWidthPx, endPaddingPx) {
                (screenWidthPx - pipWidthPx - endPaddingPx).coerceAtLeast(minX)
            }
            val draggableWidth = remember(minX, maxX) {
                (maxX - minX).coerceAtLeast(1f)
            }

            val minY = topPaddingPx
            val maxY = remember(
                pipHeightPx,
                screenHeightPx,
                bottomPaddingPx,
                isCurrentBottomScreen,
                mainState.isLandscape
            ) {
                if (!mainState.isLandscape) {
                    (screenHeightPx - pipHeightPx - bottomPaddingPx).coerceAtLeast(minY)
                } else {
                    if (isCurrentBottomScreen) {
                        (screenHeightPx - pipHeightPx - bottomPaddingPx).coerceAtLeast(minY)
                    } else {
                        (screenHeightPx - pipHeightPx).coerceAtLeast(minY)
                    }
                }
            }
            val draggableHeight = remember(minY, maxY) {
                (maxY - minY).coerceAtLeast(1f)
            }

            val pipEndDestinationPx = remember(maxX, maxY) { Offset(maxX, maxY) }
            val pipEndSizePx = remember(pipWidthPx, pipHeightPx) {
                IntSize(
                    pipWidthPx.toInt(),
                    pipHeightPx.toInt()
                )
            }


            val pipModifier = Modifier
                .align(Alignment.TopStart)
                .graphicsLayer {
                    translationX = minX + (animatableRelativeOffset.value.x * draggableWidth)
                    translationY = minY + (animatableRelativeOffset.value.y * draggableHeight)
                }
                .width(animatedPipWidth)
                .padding(8.dp)
                .aspectRatio(pipAspectRatio)
                .clip(RoundedCornerShape(8.dp))
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        onAction(
                            MainAction.SetPlayerDisplayMode(
                                if (mainState.isLandscape) PlayerDisplayMode.FULLSCREEN_LANDSCAPE
                                else PlayerDisplayMode.FULLSCREEN_PORTRAIT
                            )
                        )
                    },
                    onDoubleClick = {
                        val newWidth = if (playerState.pipWidth == 500.dp) 250.dp else 500.dp
                        onAction(MainAction.SetPlayerPipWidth(newWidth))
                    }
                )
                .pointerInput(draggableWidth, draggableHeight) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var lastVelocity = Offset.Zero

                        drag(down.id) { change ->
                            val dt = change.uptimeMillis - change.previousUptimeMillis
                            val dp = change.position - change.previousPosition
                            if (dt > 0) {
                                lastVelocity = dp / dt.toFloat()
                            }

                            val dragAmount = change.position - change.previousPosition
                            change.consume()

                            scope.launch {
                                val currentVal = animatableRelativeOffset.value
                                val newRelativeX =
                                    (currentVal.x + (dragAmount.x / draggableWidth)).coerceIn(
                                        0f, 1f
                                    )
                                val newRelativeY =
                                    (currentVal.y + (dragAmount.y / draggableHeight)).coerceIn(
                                        0f, 1f
                                    )
                                animatableRelativeOffset.snapTo(Offset(newRelativeX, newRelativeY))
                            }
                        }

                        val flingVelocityThreshold = 1.5f
                        val currentOffset = animatableRelativeOffset.value
                        var targetOffset = currentOffset

                        val isFlingX = abs(lastVelocity.x) > flingVelocityThreshold
                        val isFlingY = abs(lastVelocity.y) > flingVelocityThreshold

                        if (isFlingX || isFlingY) {
                            val targetX =
                                if (isFlingX) (if (lastVelocity.x > 0) 1f else 0f) else currentOffset.x
                            val targetY =
                                if (isFlingY) (if (lastVelocity.y > 0) 1f else 0f) else currentOffset.y
                            targetOffset = Offset(targetX, targetY)
                        }

                        scope.launch {
                            animatableRelativeOffset.animateTo(targetOffset, spring())
                            onAction(
                                MainAction.UpdatePlayerPipRelativeOffset(
                                    animatableRelativeOffset.value
                                )
                            )
                        }
                    }
                }

            val containerModifier = when (playerState.displayMode) {
                PlayerDisplayMode.PIP -> pipModifier
                else -> Modifier.fillMaxSize()
            }

            Box(modifier = containerModifier) {
                val watchViewModel: AnimeWatchViewModel = hiltViewModel()
                val watchState by watchViewModel.watchState.collectAsStateWithLifecycle()
                val playerCoreState by watchViewModel.playerCoreState.collectAsStateWithLifecycle()

                val activity = LocalActivity.current as? MainActivity

                DisposableEffect(activity) {
                    val onPictureInPictureModeChangedCallback: (Boolean) -> Unit =
                        { isInPipMode ->
                            val updatedPlayerDisplayMode =
                                if (isInPipMode) PlayerDisplayMode.SYSTEM_PIP else PlayerDisplayMode.FULLSCREEN_PORTRAIT
                            onAction(MainAction.SetPlayerDisplayMode(updatedPlayerDisplayMode))
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
                        if (isPlaying && mainState.playerState?.displayMode != PlayerDisplayMode.SYSTEM_PIP) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                    if (mainState.playerState?.displayMode == PlayerDisplayMode.SYSTEM_PIP && activity != null) {
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
                    playerDisplayMode = playerState.displayMode,
                    setPlayerDisplayMode = { onAction(MainAction.SetPlayerDisplayMode(it)) },
                    navController = navController,
                    networkDataSource = watchViewModel.networkDataSource,
                    mainState = mainState,
                    showSnackbar = { onAction(MainAction.ShowSnackbar(it)) },
                    dismissSnackbar = { onAction(MainAction.DismissSnackbar) },
                    watchState = watchState,
                    snackbarFlow = watchViewModel.snackbarFlow,
                    hlsPlayerCoreState = playerCoreState,
                    hlsControlsStateFlow = watchViewModel.controlsState,
                    onAction = watchViewModel::onAction,
                    dispatchPlayerAction = watchViewModel::dispatchPlayerAction,
                    getPlayer = watchViewModel::getPlayer,
                    captureScreenshot = { watchViewModel.captureScreenshot() },
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
                            onAction(MainAction.SetPlayerDisplayMode(PlayerDisplayMode.SYSTEM_PIP))
                        }
                    },
                    rememberedTopPadding = rememberedTopPadding,
                    rememberedBottomPadding = rememberedBottomPadding,
                    pipWidth = animatedPipWidth,
                    pipEndDestinationPx = pipEndDestinationPx,
                    pipEndSizePx = pipEndSizePx
                )

                AnimatedVisibility(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    visible = playerState.displayMode == PlayerDisplayMode.PIP,
                    enter = fadeIn(),
                    exit = fadeOut(animationSpec = tween(durationMillis = 0))
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
                    exit = fadeOut(animationSpec = tween(durationMillis = 0))
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                onClick = {
                                    watchViewModel.onAction(
                                        WatchAction.SetInitialState(
                                            playerState.malId,
                                            playerState.episodeId
                                        )
                                    )
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
    }
}