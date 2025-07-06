package com.luminoverse.animevibe.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.get
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import kotlin.math.max
import kotlin.math.min

/**
 * Enum representing common image aspect ratios with descriptive names.
 *
 * @property ratio The float value of the aspect ratio (width / height).
 */
enum class ImageAspectRatio(val ratio: Float) {
    /** A square aspect ratio (1:1). Perfect for profile pictures or thumbnails. */
    SQUARE(1f / 1f),

    /** A standard widescreen aspect ratio (16:9). Ideal for video players and landscape imagery. */
    WIDESCREEN(16f / 9f),

    /** A tall, vertical aspect ratio (9:16). Suitable for mobile screen-filling content like stories. */
    TALL_PORTRAIT(9f / 16f),

    /** A classic portrait aspect ratio (3:4). Commonly used for anime posters and standard photos. */
    POSTER(3f / 4f),

    /** A common tall poster aspect ratio (2:3). Often used for movie or promotional posters. */
    POSTER_TALL(2f / 3f)
}

enum class ImageRoundedCorner { NONE, START, END, TOP, BOTTOM, ALL }

/**
 * Converts an [ImageRoundedCorner] enum into a [RoundedCornerShape].
 * @param radius The corner radius to apply.
 */
private fun ImageRoundedCorner.toShape(radius: Dp): Shape {
    return when (this) {
        ImageRoundedCorner.NONE -> RoundedCornerShape(0.dp)
        ImageRoundedCorner.ALL -> RoundedCornerShape(radius)
        ImageRoundedCorner.TOP -> RoundedCornerShape(topStart = radius, topEnd = radius)
        ImageRoundedCorner.BOTTOM -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
        ImageRoundedCorner.START -> RoundedCornerShape(topStart = radius, bottomStart = radius)
        ImageRoundedCorner.END -> RoundedCornerShape(topEnd = radius, bottomEnd = radius)
    }
}

private fun isBitmapMostlyBlack(image: Bitmap): Boolean {
    if (image.config == Bitmap.Config.HARDWARE) {
        return false
    }
    if (image.isRecycled || image.width == 0 || image.height == 0) return true
    val maxBlackIntensity = 5
    val sampleSize = 100
    var blackPixelsFound = 0
    val totalPixelsSampled = min(image.width * image.height, sampleSize)

    repeat(totalPixelsSampled) {
        val randomX = (Math.random() * image.width).toInt()
        val randomY = (Math.random() * image.height).toInt()
        val pixel = image[randomX, randomY]
        val redChannel = (pixel shr 16) and 0xff
        val greenChannel = (pixel shr 8) and 0xff
        val blueChannel = pixel and 0xff

        if (redChannel <= maxBlackIntensity && greenChannel <= maxBlackIntensity && blueChannel <= maxBlackIntensity) {
            blackPixelsFound++
        }
    }
    return blackPixelsFound.toFloat() / totalPixelsSampled > 0.8
}

private const val PAN_ANIMATION_DURATION_MS = 8000

@Composable
fun ImageDisplay(
    modifier: Modifier = Modifier,
    image: String?,
    imagePlaceholder: String? = null,
    ratio: Float = ImageAspectRatio.POSTER_TALL.ratio,
    contentDescription: String,
    roundedCorners: ImageRoundedCorner = ImageRoundedCorner.ALL,
    isAiring: Boolean? = null,
    positionData: Pair<Long?, Long?>? = null,
    onClick: ((image: Any?, bounds: Rect, imageSize: Size?) -> Unit)? = null,
) {
    var imageBounds by remember { mutableStateOf(Rect.Zero) }
    var asyncImageSize by remember { mutableStateOf<Size?>(null) }
    var showFallback by remember(image) { mutableStateOf(image == null) }
    var componentSize by remember { mutableStateOf<Size?>(null) }

    var isVerticallyCropped by remember { mutableStateOf(false) }
    var isHorizontallyCropped by remember { mutableStateOf(false) }

    val (imageUrl, decodedBitmap) = remember(image) {
        when {
            image?.startsWith("http", ignoreCase = true) == true -> image to null
            image != null -> {
                val bitmap = try {
                    val decodedBytes = Base64.decode(image, Base64.DEFAULT)
                    val decoded = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (decoded != null && !isBitmapMostlyBlack(decoded)) decoded else null
                } catch (_: IllegalArgumentException) {
                    null
                }
                null to bitmap
            }

            else -> null to null
        }
    }

    LaunchedEffect(imageUrl, decodedBitmap) {
        if (imageUrl == null && decodedBitmap == null) {
            showFallback = true
        }
    }

    LaunchedEffect(componentSize, asyncImageSize, decodedBitmap) {
        val cSize = componentSize ?: return@LaunchedEffect

        val iSize = asyncImageSize ?: decodedBitmap?.let {
            Size(it.width.toFloat(), it.height.toFloat())
        }

        if (iSize != null) {
            val scale = max(cSize.width / iSize.width, cSize.height / iSize.height)
            val scaledImageHeight = iSize.height * scale
            val scaledImageWidth = iSize.width * scale

            val newIsVerticallyCropped = scaledImageHeight > cSize.height + 1
            val newIsHorizontallyCropped =
                !newIsVerticallyCropped && (scaledImageWidth > cSize.width + 1)

            if (newIsVerticallyCropped != isVerticallyCropped) isVerticallyCropped =
                newIsVerticallyCropped
            if (newIsHorizontallyCropped != isHorizontallyCropped) isHorizontallyCropped =
                newIsHorizontallyCropped
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ImagePanTransition")
    val panProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PAN_ANIMATION_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "PanProgress"
    )

    val alignment: Alignment = when {
        isVerticallyCropped -> BiasAlignment(0f, (panProgress * 2) - 1f)
        isHorizontallyCropped -> BiasAlignment((panProgress * 2) - 1f, 0f)
        else -> Alignment.Center
    }

    val imageModifier = Modifier
        .fillMaxSize()
        .clip(roundedCorners.toShape(radius = 8.dp))
        .then(
            if (onClick != null) {
                Modifier.clickable {
                    val bestSize = asyncImageSize ?: componentSize
                    val (img: Any?, size: Size?) = when {
                        showFallback -> imagePlaceholder to bestSize
                        decodedBitmap != null -> decodedBitmap.asImageBitmap() to Size(
                            decodedBitmap.width.toFloat(),
                            decodedBitmap.height.toFloat()
                        )

                        imageUrl != null -> imageUrl to bestSize
                        else -> null to null
                    }
                    if (img != null) {
                        onClick(img, imageBounds, size)
                    }
                }
            } else Modifier
        )

    Box(
        modifier = modifier
            .aspectRatio(ratio)
            .onGloballyPositioned { layoutCoordinates ->
                imageBounds = layoutCoordinates.boundsInWindow()
                componentSize = Size(
                    layoutCoordinates.size.width.toFloat(),
                    layoutCoordinates.size.height.toFloat()
                )
            }
    ) {
        if (showFallback) {
            if (imagePlaceholder != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imagePlaceholder)
                        .build(),
                    contentDescription = "Placeholder for $contentDescription",
                    modifier = imageModifier.align(Alignment.Center),
                    contentScale = ContentScale.Crop,
                    alignment = alignment,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Success) {
                            asyncImageSize = state.painter.intrinsicSize
                        }
                    }
                )
            } else {
                Icon(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    imageVector = Icons.Filled.Image,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        } else {
            when {
                decodedBitmap != null -> {
                    Image(
                        bitmap = decodedBitmap.asImageBitmap(),
                        contentDescription = contentDescription,
                        modifier = imageModifier.align(Alignment.Center),
                        contentScale = ContentScale.Crop,
                        alignment = alignment
                    )
                }

                imageUrl != null -> {
                    var isImageLoading by remember(imageUrl) { mutableStateOf(true) }

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .allowHardware(false)
                            .build(),
                        contentDescription = contentDescription,
                        modifier = imageModifier.align(Alignment.Center),
                        contentScale = ContentScale.Crop,
                        alignment = alignment,
                        onState = { state ->
                            isImageLoading = state is AsyncImagePainter.State.Loading
                            when (state) {
                                is AsyncImagePainter.State.Success -> {
                                    asyncImageSize = state.painter.intrinsicSize
                                    val bitmap = state.result.drawable.toBitmap()
                                    if (isBitmapMostlyBlack(bitmap)) {
                                        showFallback = true
                                    }
                                }

                                is AsyncImagePainter.State.Error -> showFallback = true
                                else -> {}
                            }
                        }
                    )

                    if (isImageLoading) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "Placeholder",
                            tint = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }

        if (isAiring != null) {
            val icon =
                if (isAiring) Icons.Filled.NotificationsActive else Icons.Filled.Done
            val tint =
                if (isAiring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            Icon(
                imageVector = icon,
                contentDescription = if (isAiring) "Airing" else "Finished",
                tint = tint,
                modifier = Modifier
                    .padding(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(4.dp)
                    .size(16.dp)
                    .align(Alignment.TopStart)
            )
        }

        positionData?.let {
            it.first?.let { currentPosition ->
                it.second?.let { totalDuration ->
                    if (totalDuration <= 0) return@let
                    val progress = (currentPosition.toFloat() / totalDuration).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    }
}