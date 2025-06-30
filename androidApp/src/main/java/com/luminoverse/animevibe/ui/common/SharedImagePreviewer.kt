package com.luminoverse.animevibe.ui.common

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * Represents the state for a shared element transition, including the image,
 * its description, and its initial bounding box on the screen.
 */
data class SharedImageState(
    val image: Any?,
    val contentDescription: String?,
    val initialBounds: Rect,
    val initialSize: Size?
)

/**
 * A helper function to safely find the host Activity from a Context.
 */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * A full-screen overlay that displays an image, animating it from its initial position
 * and size to a centered, full-screen preview with uniform scaling.
 *
 * @param sharedImageState The state containing the image and its initial bounds.
 * @param onDismiss Request to dismiss the previewer, triggering the exit animation.
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun SharedImagePreviewer(
    sharedImageState: SharedImageState,
    onDismiss: () -> Unit
) {
    val previewerState = rememberSharedImagePreviewerState()

    LaunchedEffect(Unit) {
        previewerState.enter()
    }

    BackHandler { previewerState.exit() }

    if (previewerState.isAnimatingOut) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(300)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { previewerState.exit() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val density = LocalDensity.current
        val config = LocalConfiguration.current
        val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

        val initialBounds = sharedImageState.initialBounds
        val initialSize = sharedImageState.initialSize ?: Size(1f, 1f)

        val imageAspectRatio =
            if (initialSize.height > 0f) initialSize.width / initialSize.height else 1f
        val screenAspectRatio = screenWidthPx / screenHeightPx

        val (fittedWidth, fittedHeight) = if (imageAspectRatio > screenAspectRatio) {
            Pair(config.screenWidthDp.dp, config.screenWidthDp.dp / imageAspectRatio)
        } else {
            Pair(config.screenHeightDp.dp * imageAspectRatio, config.screenHeightDp.dp)
        }

        val targetWidth = with(density) { initialBounds.width.toDp() }
        val targetHeight = with(density) { initialBounds.height.toDp() }

        val animatedWidth by animateDpAsState(
            targetValue = if (previewerState.isVisible) fittedWidth else targetWidth,
            animationSpec = spring(dampingRatio = 0.8f),
            label = "width"
        )
        val animatedHeight by animateDpAsState(
            targetValue = if (previewerState.isVisible) fittedHeight else targetHeight,
            animationSpec = spring(dampingRatio = 0.8f),
            label = "height"
        )
        val animatedCornerRadius by animateDpAsState(
            targetValue = if (previewerState.isVisible) 0.dp else 8.dp,
            animationSpec = spring(dampingRatio = 0.8f),
            label = "cornerRadius"
        )

        val initialCenterX = initialBounds.left + initialBounds.width / 2
        val screenCenterX = screenWidthPx / 2
        val translationX = initialCenterX - screenCenterX

        val initialCenterY = initialBounds.top + initialBounds.height / 2
        val screenCenterY = screenHeightPx / 2
        val translationY = initialCenterY - screenCenterY

        val animatedTranslationX by animateFloatAsState(
            if (previewerState.isVisible) 0f else translationX,
            spring(dampingRatio = 0.8f),
            label = "translateX"
        )
        val animatedTranslationY by animateFloatAsState(
            if (previewerState.isVisible) 0f else translationY,
            spring(dampingRatio = 0.8f),
            label = "translateY"
        )
        val animatedAlpha by animateFloatAsState(
            if (previewerState.isVisible) 1f else 0f,
            spring(),
            label = "alpha"
        )

        val animatedZoom by animateFloatAsState(
            targetValue = previewerState.zoomScale,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "ImageZoomAnimation"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = animatedAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(animatedWidth.coerceAtLeast(0.dp))
                    .height(animatedHeight.coerceAtLeast(0.dp))
                    .onSizeChanged { previewerState.updateContainerSize(it) }
                    .graphicsLayer {
                        scaleX = animatedZoom
                        scaleY = animatedZoom
                        this.translationX = animatedTranslationX + previewerState.panOffset.x
                        this.translationY = animatedTranslationY + previewerState.panOffset.y
                        shape = RoundedCornerShape(animatedCornerRadius.coerceAtLeast(0.dp))
                        clip = true
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val currentTime = System.currentTimeMillis()

                            if (currentTime - previewerState.lastTapTime < DOUBLE_TAP_THRESHOLD_MILLIS &&
                                (previewerState.lastTapPosition - down.position).getDistanceSquared() < 100f * 100f
                            ) {
                                previewerState.handleDoubleTap(down.position)
                            } else {
                                do {
                                    val event = awaitPointerEvent()
                                    val zoom = event.calculateZoom()
                                    if (zoom != 1f) {
                                        previewerState.handleZoom(zoom)
                                    }
                                    if (previewerState.zoomScale > 1f) {
                                        val pan = event.calculatePan()
                                        previewerState.handlePan(pan)
                                    }
                                } while (event.changes.any { it.pressed })

                                previewerState.onGestureEnd()
                            }
                            previewerState.lastTapTime = currentTime
                            previewerState.lastTapPosition = down.position
                        }
                    }
            ) {
                when (val image = sharedImageState.image) {
                    is ImageBitmap -> {
                        Image(
                            bitmap = image,
                            contentDescription = sharedImageState.contentDescription,
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged {
                                    previewerState.updateImageSize(
                                        Size(
                                            it.width.toFloat(),
                                            it.height.toFloat()
                                        )
                                    )
                                },
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter
                        )
                    }

                    else -> {
                        AsyncImage(
                            model = image,
                            contentDescription = sharedImageState.contentDescription,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            onSuccess = { state ->
                                previewerState.updateImageSize(state.painter.intrinsicSize)
                            }
                        )
                    }
                }
            }

            if (animatedCornerRadius == 0.dp) Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Preview",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { previewerState.exit() }
                    .padding(8.dp)
            )
        }
    }
}