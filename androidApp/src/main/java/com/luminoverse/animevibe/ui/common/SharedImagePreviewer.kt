package com.luminoverse.animevibe.ui.common

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage

/**
 * Represents the state for a shared element transition, including the image,
 * its description, and its initial bounding box on the screen.
 */
data class SharedImageState(
    val image: Any?,
    val contentDescription: String?,
    val initialBounds: Rect
)

/**
 * A full-screen overlay that displays an image, animating it from its initial position
 * and size to a centered, full-screen preview.
 *
 * @param sharedImageState The state containing the image and its initial bounds.
 * @param onDismiss Request to dismiss the previewer, triggering the exit animation.
 */
@Composable
fun SharedImagePreviewer(
    sharedImageState: SharedImageState,
    onDismiss: () -> Unit
) {
    var isAnimatingIn by remember { mutableStateOf(false) }
    var isAnimatingOut by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)

        insetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    if (isAnimatingOut) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(300)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        isAnimatingIn = true
    }

    BackHandler {
        isAnimatingIn = false
        isAnimatingOut = true
    }

    Dialog(
        onDismissRequest = {
            isAnimatingIn = false
            isAnimatingOut = true
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

        val initialWidth = sharedImageState.initialBounds.width
        val initialHeight = sharedImageState.initialBounds.height
        val aspectRatio = if (initialHeight > 0) initialWidth / initialHeight else 1f

        val finalWidth: Float
        val finalHeight: Float

        if (aspectRatio > (screenWidthPx / screenHeightPx)) {
            finalWidth = screenWidthPx
            finalHeight = screenWidthPx / aspectRatio
        } else {
            finalHeight = screenHeightPx
            finalWidth = screenHeightPx * aspectRatio
        }

        val targetScaleX = if (isAnimatingIn) 1f else initialWidth / finalWidth
        val targetScaleY = if (isAnimatingIn) 1f else initialHeight / finalHeight
        val targetAlpha = if (isAnimatingIn) 0.95f else 0f

        val screenCenterX = screenWidthPx / 2
        val screenCenterY = screenHeightPx / 2

        val initialCenterX = sharedImageState.initialBounds.left + initialWidth / 2
        val initialCenterY = sharedImageState.initialBounds.top + initialHeight / 2

        val targetTranslationX = if (isAnimatingIn) 0f else initialCenterX - screenCenterX
        val targetTranslationY = if (isAnimatingIn) 0f else initialCenterY - screenCenterY

        val scaleX by animateFloatAsState(targetScaleX, spring(dampingRatio = 0.8f), label = "")
        val scaleY by animateFloatAsState(targetScaleY, spring(dampingRatio = 0.8f), label = "")
        val alpha by animateFloatAsState(targetAlpha, spring(), label = "")
        val translationX by animateFloatAsState(targetTranslationX, spring(dampingRatio = 0.8f), label = "")
        val translationY by animateFloatAsState(targetTranslationY, spring(dampingRatio = 0.8f), label = "")

        val animatedCornerRadius by animateDpAsState(
            targetValue = if (isAnimatingIn) 16.dp else 8.dp,
            animationSpec = spring(dampingRatio = 0.8f),
            label = "CornerRadiusAnimation"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = alpha)),
            contentAlignment = Alignment.Center
        ) {
            val imageModifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    this.translationX = translationX
                    this.translationY = translationY
                }
                .clip(RoundedCornerShape(animatedCornerRadius))

            when (val image = sharedImageState.image) {
                is ImageBitmap -> {
                    Image(
                        bitmap = image,
                        contentDescription = sharedImageState.contentDescription,
                        modifier = imageModifier,
                        contentScale = ContentScale.Fit
                    )
                }
                else -> {
                    AsyncImage(
                        model = image,
                        contentDescription = sharedImageState.contentDescription,
                        modifier = imageModifier,
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Preview",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        isAnimatingIn = false
                        isAnimatingOut = true
                    }
                    .padding(8.dp)
            )
        }
    }
}