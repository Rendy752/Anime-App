package com.luminoverse.animevibe.ui.common

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.coerceAtLeast
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

    LaunchedEffect(Unit) {
        isAnimatingIn = true
    }

    fun close() {
        isAnimatingIn = false
        isAnimatingOut = true
    }

    BackHandler { close() }

    if (isAnimatingOut) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(300)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { close() },
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
            targetValue = if (isAnimatingIn) fittedWidth else targetWidth,
            animationSpec = spring(dampingRatio = 0.8f),
            label = "width"
        )
        val animatedHeight by animateDpAsState(
            targetValue = if (isAnimatingIn) fittedHeight else targetHeight,
            animationSpec = spring(dampingRatio = 0.8f),
            label = "height"
        )
        val animatedCornerRadius by animateDpAsState(
            targetValue = if (isAnimatingIn) 0.dp else 8.dp,
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
            if (isAnimatingIn) 0f else translationX,
            spring(dampingRatio = 0.8f),
            label = "translateX"
        )
        val animatedTranslationY by animateFloatAsState(
            if (isAnimatingIn) 0f else translationY,
            spring(dampingRatio = 0.8f),
            label = "translateY"
        )
        val animatedAlpha by animateFloatAsState(
            if (isAnimatingIn) 1f else 0f,
            spring(),
            label = "alpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = animatedAlpha)),
            contentAlignment = Alignment.Center
        ) {
            val imageModifier = Modifier
                .width(animatedWidth.coerceAtLeast(0.dp))
                .height(animatedHeight.coerceAtLeast(0.dp))
                .graphicsLayer {
                    this.translationX = animatedTranslationX
                    this.translationY = animatedTranslationY
                    this.shape = RoundedCornerShape(animatedCornerRadius.coerceAtLeast(0.dp))
                    this.clip = true
                }

            when (val image = sharedImageState.image) {
                is ImageBitmap -> {
                    Image(
                        bitmap = image,
                        contentDescription = sharedImageState.contentDescription,
                        modifier = imageModifier,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                    )
                }

                else -> {
                    AsyncImage(
                        model = image,
                        contentDescription = sharedImageState.contentDescription,
                        modifier = imageModifier,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Preview",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { close() }
                    .padding(8.dp)
            )
        }
    }
}