package com.luminoverse.animevibe.ui.main

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(UnstableApi::class)
@Composable
fun CustomPipPlayer(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    navController: NavHostController, // Use NavController from MainActivity
    hlsPlayerUtils: HlsPlayerUtils
) {
    val pipState by mainViewModel.state.map { it.customPipState }.collectAsStateWithLifecycle(null)
    val playerCoreState by hlsPlayerUtils.playerCoreState.collectAsStateWithLifecycle()

    pipState?.let { state ->
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

        var pipSize by remember { mutableStateOf(IntSize.Zero) }

        Box(
            modifier = modifier
                .offset { state.offset }
                .onSizeChanged { pipSize = it }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newOffset = state.offset + IntOffset(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
                        val constrainedX = newOffset.x.toFloat().coerceIn(0f, screenWidth - pipSize.width)
                        val constrainedY = newOffset.y.toFloat().coerceIn(0f, screenHeight - pipSize.height)
                        mainViewModel.onAction(MainAction.UpdateCustomPipOffset(IntOffset(constrainedX.roundToInt(), constrainedY.roundToInt())))
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(112.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable {
                        // When clicked, navigate to the watch screen and exit PiP mode
                        navController.navigateTo(NavRoute.AnimeWatch.fromParams(state.malId, state.episodeId))
                        mainViewModel.onAction(MainAction.ExitCustomPipMode)
                    }
            ) {
                val player = hlsPlayerUtils.getPlayer()

                AndroidView(
                    factory = { context -> PlayerView(context).apply { useController = false } },
                    update = { playerView -> playerView.player = player },
                    onRelease = { playerView -> playerView.player = null },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                ) {
                    IconButton(
                        onClick = {
                            if (playerCoreState.isPlaying) hlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
                            else hlsPlayerUtils.dispatch(HlsPlayerAction.Play)
                        },
                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) { Icon(if (playerCoreState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.White) }

                    IconButton(
                        onClick = {
                            hlsPlayerUtils.dispatch(HlsPlayerAction.Reset)
                            mainViewModel.onAction(MainAction.ExitCustomPipMode)
                        },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) { Icon(Icons.Default.Close, "Close PiP", tint = Color.White) }
                }
            }
        }
    }
}