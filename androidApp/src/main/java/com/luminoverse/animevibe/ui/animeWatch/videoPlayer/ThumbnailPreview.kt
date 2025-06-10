package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import com.luminoverse.animevibe.utils.TimeUtils.parseTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import android.util.Log
import androidx.compose.material3.MaterialTheme

data class VttCue(
    val startTime: Long,
    val endTime: Long,
    val imageUrl: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

suspend fun parseVtt(vttContent: String, baseUrl: String): List<VttCue> =
    withContext(Dispatchers.Default) {
        val cues = mutableListOf<VttCue>()
        val lines = vttContent.split("\n").map { it.trim() }
        var i = 0
        while (i < lines.size) {
            if (lines[i].matches("\\d+".toRegex())) {
                i++
                if (i >= lines.size) break
            }

            if (lines[i].contains("-->")) {
                val parts = lines[i].split("-->").map { it.trim() }
                if (parts.size == 2) {
                    val startTime = parseTimestamp(parts[0])
                    val endTime = parseTimestamp(parts[1])
                    i++
                    if (i >= lines.size) break

                    val imageUrlLine = lines[i]
                    val imageUrlParts = imageUrlLine.split("#xywh=")
                    if (imageUrlParts.size == 2) {
                        val relativeImageUrl = imageUrlParts[0]
                        val fullImageUrl =
                            if (relativeImageUrl.startsWith("http://") || relativeImageUrl.startsWith(
                                    "https://"
                                )
                            ) {
                                relativeImageUrl
                            } else {
                                val base = URL(baseUrl)
                                URL(base, relativeImageUrl).toString()
                            }

                        val xywhParts = imageUrlParts[1].split(",").map { it.toInt() }
                        if (xywhParts.size == 4) {
                            cues.add(
                                VttCue(
                                    startTime = startTime,
                                    endTime = endTime,
                                    imageUrl = fullImageUrl,
                                    x = xywhParts[0],
                                    y = xywhParts[1],
                                    width = xywhParts[2],
                                    height = xywhParts[3]
                                )
                            )
                        }
                    }
                }
            }
            i++
        }
        cues
    }

fun findCueForPosition(position: Long, cues: List<VttCue>): VttCue? {
    return cues.find { position >= it.startTime && position < it.endTime }
}

class CropTransformation(private val rect: Rect) : Transformation {
    override val cacheKey: String get() = "CropTransformation(rect=$rect)"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return Bitmap.createBitmap(input, rect.left, rect.top, rect.width(), rect.height())
    }
}

@Composable
fun ThumbnailPreview(
    modifier: Modifier = Modifier,
    seekPosition: Long,
    thumbnailTrackUrl: String,
) {
    var vttCues by remember(thumbnailTrackUrl) { mutableStateOf<List<VttCue>?>(null) }
    val context = LocalContext.current

    LaunchedEffect(thumbnailTrackUrl) {
        try {
            val vttContent = withContext(Dispatchers.IO) {
                URL(thumbnailTrackUrl).readText()
            }
            val baseUrl = thumbnailTrackUrl.substringBeforeLast("/") + "/"
            vttCues = parseVtt(vttContent, baseUrl)
        } catch (e: Exception) {
            Log.e("ThumbnailPreview", "Failed to fetch or parse VTT: ${e.message}", e)
            vttCues = emptyList()
        }
    }

    val currentCue = vttCues?.let { cues ->
        findCueForPosition(seekPosition, cues)
    }

    val imageUrl = currentCue?.imageUrl
    val cropRect = currentCue?.let { Rect(it.x, it.y, it.x + it.width, it.y + it.height) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null && cropRect != null) {
            val painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(Size.ORIGINAL)
                    .transformations(listOf(CropTransformation(cropRect)))
                    .crossfade(true)
                    .build(),
                imageLoader = ImageLoader(context)
            )
            Image(
                painter = painter,
                contentDescription = "Thumbnail Preview",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}