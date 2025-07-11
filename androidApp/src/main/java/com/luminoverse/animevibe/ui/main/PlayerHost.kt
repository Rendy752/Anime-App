package com.luminoverse.animevibe.ui.main

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.ui.animeWatch.AnimeWatchScreen
import com.luminoverse.animevibe.ui.animeWatch.AnimeWatchViewModel
import com.luminoverse.animevibe.ui.animeWatch.WatchAction
import com.luminoverse.animevibe.ui.animeWatch.components.InfoContentSection
import com.luminoverse.animevibe.ui.animeWatch.watchContent.WatchContentSection
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import com.luminoverse.animevibe.utils.media.PipUtil.buildPipActions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

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
        val watchViewModel: AnimeWatchViewModel = hiltViewModel()
        val watchState by watchViewModel.watchState.collectAsStateWithLifecycle()
        val playerCoreState by watchViewModel.playerCoreState.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()

        LaunchedEffect(playerState.malId, playerState.episodeId) {
            hlsPlayerUtils.dispatch(HlsPlayerAction.Reset)
            watchViewModel.onAction(
                WatchAction.SetInitialState(
                    playerState.malId, playerState.episodeId
                )
            )
            scope.launch {
                watchViewModel.snackbarFlow.collectLatest { snackbarMessage ->
                    onAction(MainAction.ShowSnackbar(snackbarMessage))
                }
            }
        }

        val activity = LocalActivity.current as? MainActivity
        DisposableEffect(activity) {
            val onPictureInPictureModeChangedCallback: (Boolean) -> Unit =
                { isInPipMode ->
                    val updatedPlayerDisplayMode =
                        if (isInPipMode) PlayerDisplayMode.SYSTEM_PIP else PlayerDisplayMode.FULLSCREEN_PORTRAIT
                    onAction(MainAction.SetPlayerDisplayMode(updatedPlayerDisplayMode))
                    Log.d("MainScreen", "PiP mode changed: isInPipMode=$isInPipMode")
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

        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenWidthDp = configuration.screenWidthDp.dp
        val screenWidthPx = with(density) { screenWidthDp.toPx() }
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

        val animatableRelativeOffset =
            remember { Animatable(playerState.pipRelativeOffset, Offset.VectorConverter) }

        LaunchedEffect(playerState.pipRelativeOffset) {
            if (animatableRelativeOffset.value != playerState.pipRelativeOffset) {
                animatableRelativeOffset.snapTo(playerState.pipRelativeOffset)
            }
        }

        val verticalDragOffset = remember { Animatable(0f) }
        LaunchedEffect(mainState.isLandscape) {
            if (mainState.isLandscape) verticalDragOffset.snapTo(0f)
        }
        var maxVerticalDrag by remember { mutableFloatStateOf(Float.POSITIVE_INFINITY) }

        val dismissDragThreshold =
            if (maxVerticalDrag.isFinite()) maxVerticalDrag else screenHeightPx
        val pipDragProgress = (verticalDragOffset.value / dismissDragThreshold).coerceIn(0f, 1f)

        val topPaddingPx = with(density) { rememberedTopPadding.toPx() }
        val bottomPaddingPx = with(density) { rememberedBottomPadding.toPx() }
        val startPaddingPx = with(density) { startPadding.toPx() }
        val endPaddingPx = with(density) { endPadding.toPx() }

        // 1. Determine the maximum safe width for the PiP window on the current screen.
        val safeScreenWidthDp = screenWidthDp - (startPadding + endPadding)

        // 2. Constrain the desired PiP width to not exceed this safe screen width.
        val coercedPipWidth = playerState.pipWidth.coerceAtMost(safeScreenWidthDp)

        // 3. Use this coerced (corrected) width as the animation target.
        val animatedPipWidth by animateDpAsState(
            targetValue = coercedPipWidth,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
            label = "pipWidth"
        )

        // ✨ END FIX ✨

        val videoAspectRatio = 16f / 9f

        // All subsequent calculations will now be correct because they start from a valid width.
        val pipWidthPx = with(density) { animatedPipWidth.toPx() }
        val pipHeightPx = with(density) { (animatedPipWidth / videoAspectRatio).toPx() }

        val minX = startPaddingPx
        val maxX = (screenWidthPx - pipWidthPx - endPaddingPx).coerceAtLeast(minX)
        val draggableWidth = (maxX - minX).coerceAtLeast(1f)

        val minY = topPaddingPx
        val maxY = if (!mainState.isLandscape) {
            (screenHeightPx - pipHeightPx - bottomPaddingPx).coerceAtLeast(minY)
        } else {
            if (isCurrentBottomScreen) {
                (screenHeightPx - pipHeightPx - bottomPaddingPx).coerceAtLeast(minY)
            } else {
                (screenHeightPx - pipHeightPx).coerceAtLeast(minY)
            }
        }
        val draggableHeight = (maxY - minY).coerceAtLeast(1f)

        val pipEndDestinationPx = Offset(maxX, maxY)
        val pipEndSizePx = IntSize(pipWidthPx.toInt(), pipHeightPx.toInt())

        val pipDragModifier = Modifier.pointerInput(draggableWidth, draggableHeight) {
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
                        val newRelativeX = currentVal.x + (dragAmount.x / draggableWidth)
                        val newRelativeY = currentVal.y + (dragAmount.y / draggableHeight)
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
                val correctedTargetOffset =
                    Offset(x = targetOffset.x.coerceIn(0f, 1f), y = targetOffset.y.coerceIn(0f, 1f))
                scope.launch {
                    animatableRelativeOffset.animateTo(correctedTargetOffset, spring())
                    onAction(MainAction.UpdatePlayerPipRelativeOffset(animatableRelativeOffset.value))
                }
            }
        }

        val pipPlacementModifier = Modifier
            .graphicsLayer {
                translationX = minX + (animatableRelativeOffset.value.x * draggableWidth)
                translationY = minY + (animatableRelativeOffset.value.y * draggableHeight)
            }
            .width(animatedPipWidth)
            .padding(8.dp)
            .aspectRatio(videoAspectRatio)
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
            .then(pipDragModifier)

        val isPipMode =
            playerState.displayMode in listOf(PlayerDisplayMode.SYSTEM_PIP, PlayerDisplayMode.PIP)
        val animatedBackgroundColor by animateColorAsState(
            targetValue = MaterialTheme.colorScheme.background.copy(
                alpha = 1f - if (isPipMode) 1f else pipDragProgress
            ),
            animationSpec = tween(durationMillis = 150),
            label = "backgroundAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedBackgroundColor)
                .padding(
                    top = if (mainState.isLandscape || isPipMode) 0.dp else rememberedTopPadding,
                    bottom = if (mainState.isLandscape || isPipMode) 0.dp else rememberedBottomPadding
                )
        ) {
            val serverScrollState = rememberScrollState()
            val columnScrollState = rememberScrollState()
            val defaultPlayerHeight = screenWidthDp * (9f / 16f)
            Column(
                modifier = Modifier
                    .padding(top = if (isPipMode) configuration.screenHeightDp.dp else defaultPlayerHeight)
                    .graphicsLayer {
                        translationY = verticalDragOffset.value.coerceAtLeast(0f)
                        alpha = 1f - (pipDragProgress * 1.5f).coerceIn(0f, 1f)
                    }
                    .fillMaxSize()
                    .blur(radius = (pipDragProgress * 10.dp).coerceAtMost(10.dp))
                    .padding(horizontal = 8.dp)
                    .verticalScroll(columnScrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (watchState.animeDetailComplement?.episodes != null
                    && watchState.animeDetail?.mal_id == playerState.malId
                ) WatchContentSection(
                    animeDetail = watchState.animeDetail,
                    networkStatus = mainState.networkStatus,
                    onFavoriteToggle = { isFavorite ->
                        watchViewModel.onAction(
                            WatchAction.SetFavorite(isFavorite)
                        )
                    },
                    episodeDetailComplement = watchState.episodeDetailComplement,
                    onLoadEpisodeDetailComplement = {
                        watchViewModel.onAction(
                            WatchAction.LoadEpisodeDetailComplement(it)
                        )
                    },
                    episodeDetailComplements = watchState.episodeDetailComplements,
                    episodes = watchState.animeDetailComplement?.episodes ?: emptyList(),
                    newEpisodeIdList = watchState.newEpisodeIdList,
                    episodeSourcesQuery = watchState.episodeSourcesQuery,
                    episodeJumpNumber = watchState.episodeJumpNumber,
                    setEpisodeJumpNumber = {
                        watchViewModel.onAction(
                            WatchAction.SetEpisodeJumpNumber(
                                it
                            )
                        )
                    },
                    serverScrollState = serverScrollState,
                    isError = playerCoreState.error != null,
                    isRefreshing = watchState.isRefreshing,
                    handleSelectedEpisodeServer = { episodeSourcesQuery, isFirstInit, isRefresh ->
                        watchViewModel.onAction(
                            WatchAction.HandleSelectedEpisodeServer(
                                episodeSourcesQuery = episodeSourcesQuery,
                                isFirstInit = isFirstInit,
                                isRefresh = isRefresh
                            )
                        )
                    },
                )
                InfoContentSection(
                    animeDetail = watchState.animeDetail,
                    navController = navController,
                    setPlayerDisplayMode = { onAction(MainAction.SetPlayerDisplayMode(it)) }
                )
            }

            if (mainState.isLandscape && !isPipMode) Spacer(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxSize()
            )

            AnimeWatchScreen(
                modifier = if (playerState.displayMode == PlayerDisplayMode.PIP) pipPlacementModifier else Modifier,
                malId = playerState.malId,
                episodeId = playerState.episodeId,
                playerDisplayMode = playerState.displayMode,
                setPlayerDisplayMode = { onAction(MainAction.SetPlayerDisplayMode(it)) },
                navController = navController,
                networkDataSource = watchViewModel.networkDataSource,
                mainState = mainState,
                showSnackbar = { onAction(MainAction.ShowSnackbar(it)) },
                dismissSnackbar = { onAction(MainAction.DismissSnackbar) },
                closePlayer = { onAction(MainAction.ClosePlayer) },
                watchState = watchState,
                hlsPlayerCoreState = playerCoreState,
                hlsControlsStateFlow = watchViewModel.controlsState,
                onAction = watchViewModel::onAction,
                dispatchPlayerAction = watchViewModel::dispatchPlayerAction,
                getPlayer = watchViewModel::getPlayer,
                captureScreenshot = { watchViewModel.captureScreenshot() },
                onEnterSystemPipMode = {
                    if (activity != null) {
                        val actions = buildPipActions(activity, playerCoreState.isPlaying)
                        activity.enterPictureInPictureMode(
                            PictureInPictureParams.Builder().setActions(actions).build()
                        )
                        onAction(MainAction.SetPlayerDisplayMode(PlayerDisplayMode.SYSTEM_PIP))
                    }
                },
                rememberedTopPadding = rememberedTopPadding,
                screenHeightPx = screenHeightPx,
                verticalDragOffset = verticalDragOffset,
                pipDragProgress = pipDragProgress,
                maxVerticalDrag = maxVerticalDrag,
                setMaxVerticalDrag = { maxVerticalDrag = it },
                pipWidth = animatedPipWidth,
                pipEndDestinationPx = pipEndDestinationPx,
                pipEndSizePx = pipEndSizePx
            )

            if (pipDragProgress > 0f && mainState.playerState?.displayMode == PlayerDisplayMode.FULLSCREEN_PORTRAIT) {
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