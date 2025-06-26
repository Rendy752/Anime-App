package com.luminoverse.animevibe.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.animeDetailPlaceholder
import androidx.core.graphics.get

@Preview
@Composable
fun ScreenshotDisplay(
    modifier: Modifier = Modifier,
    imageUrl: String? = animeDetailPlaceholder.images.webp.large_image_url,
    screenshot: String? = "",
    positionData: Pair<Long?, Long?>? = null,
    onClick: (() -> Unit)? = null,
) {
    var showDialog by remember { mutableStateOf(false) }

    fun isBitmapMostlyBlack(bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled || bitmap.width == 0 || bitmap.height == 0) return true
        val threshold = 5
        val sampleSize = 100
        var blackPixelCount = 0
        val totalPixelsToSample = minOf(bitmap.width * bitmap.height, sampleSize)

        repeat(totalPixelsToSample) {
            val x = (Math.random() * bitmap.width).toInt()
            val y = (Math.random() * bitmap.height).toInt()
            val pixel = bitmap[x, y]
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff

            if (r <= threshold && g <= threshold && b <= threshold) {
                blackPixelCount++
            }
        }
        return blackPixelCount.toFloat() / totalPixelsToSample > 0.8
    }

    val screenshotBitmap = screenshot?.let { base64String ->
        try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            if (bitmap != null && !isBitmapMostlyBlack(bitmap)) bitmap
            else null
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
    ) {
        when {
            screenshotBitmap != null -> {
                Image(
                    bitmap = screenshotBitmap.asImageBitmap(),
                    contentDescription = "Episode Screenshot",
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (onClick != null) {
                                onClick()
                            } else {
                                showDialog = true
                            }
                        }
                )
            }

            imageUrl != null -> {
                AsyncImageWithPlaceholder(
                    model = imageUrl,
                    contentDescription = "Episode Image URL",
                    modifier = modifier.fillMaxSize(),
                    onClick = onClick,
                )
            }

            else -> {
                Icon(
                    modifier = modifier.fillMaxSize(),
                    imageVector = Icons.Filled.Image,
                    contentDescription = "Placeholder",
                    tint = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
        positionData?.let {
            it.first?.let { lastTimestamp ->
                it.second?.let { duration ->
                    val progress = if (lastTimestamp < duration) {
                        (lastTimestamp.toFloat() / duration).coerceIn(0f, 1f)
                    } else {
                        null
                    }
                    if (progress == null) return@let
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

    if (showDialog) {
        ImagePreviewDialog(
            image = screenshotBitmap?.asImageBitmap() ?: imageUrl,
            contentDescription = "Episode Screenshot Preview",
            onDismiss = { showDialog = false }
        )
    }
}