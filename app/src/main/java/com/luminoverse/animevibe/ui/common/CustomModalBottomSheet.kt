package com.luminoverse.animevibe.ui.common

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

data class BottomSheetConfig(
    val landscapeWidthFraction: Float = 0.7f,
    val landscapeHeightFraction: Float = 0.9f,
    val portraitWidthFraction: Float = 0.95f,
    val portraitHeightFraction: Float = 0.6f
)

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun CustomModalBottomSheet(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isLandscape: Boolean,
    config: BottomSheetConfig = BottomSheetConfig(),
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val bottomSheetWidthFraction =
        if (isLandscape) config.landscapeWidthFraction else config.portraitWidthFraction
    val bottomSheetHeightFraction =
        if (isLandscape) config.landscapeHeightFraction else config.portraitHeightFraction
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val shape = RoundedCornerShape(24.dp)
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val maxHeightPx =
        with(density) { (configuration.screenHeightDp * bottomSheetHeightFraction).dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val maxDownwardOffsetPx = screenHeightPx - maxHeightPx
    val minSwipeDistancePx = with(density) { 150.dp.toPx() }
    var swipeOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            swipeOffsetY = 0f
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300)),
        modifier = modifier.fillMaxSize()
    ) {
        Popup(
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(300)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset { IntOffset(0, swipeOffsetY.toInt()) }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (swipeOffsetY > minSwipeDistancePx) {
                                        onDismiss()
                                    } else {
                                        coroutineScope.launch {
                                            swipeOffsetY = 0f
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch { swipeOffsetY = 0f }
                                }
                            ) { change, dragAmount ->
                                val newOffset = swipeOffsetY + dragAmount
                                swipeOffsetY = max(0f, min(maxDownwardOffsetPx, newOffset))
                                change.consume()
                            }
                        }
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = (configuration.screenWidthDp * bottomSheetWidthFraction).dp)
                            .heightIn(max = (configuration.screenHeightDp * bottomSheetHeightFraction).dp)
                            .fillMaxWidth()
                            .padding(bottom = if (isLandscape) 4.dp else 24.dp),
                        shape = shape,
                        color = containerColor,
                        tonalElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 8.dp)
                                    .width(32.dp)
                                    .height(4.dp)
                                    .background(
                                        color = Color.Gray.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            content()
                        }
                    }
                }
            }
        }
    }
}