package com.luminoverse.animevibe.utils.media

import android.graphics.Bitmap
import android.graphics.Rect
import coil.size.Size
import coil.transform.Transformation
import com.luminoverse.animevibe.utils.TimeUtils.parseTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class ThumbnailCue(
    val startTime: Long,
    val endTime: Long,
    val imageUrl: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class CaptionCue(
    val startTime: Long,
    val endTime: Long,
    val text: String
)

/**
 * Parses a VTT file content specifically for thumbnail sprites.
 * @param vttContent The raw string content of the VTT file.
 * @param baseUrl The base URL to resolve relative image paths.
 * @return A list of [ThumbnailCue] objects.
 */
suspend fun parseThumbnailCues(vttContent: String, baseUrl: String): List<ThumbnailCue> =
    withContext(Dispatchers.Default) {
        val cues = mutableListOf<ThumbnailCue>()
        val lines = vttContent.split("\n").map { it.trim() }
        var i = 0
        while (i < lines.size) {
            if (lines[i].contains("-->")) {
                val timeParts = lines[i].split("-->").map { it.trim() }
                if (timeParts.size == 2) {
                    val startTime = parseTimestamp(timeParts[0])
                    val endTime = parseTimestamp(timeParts[1])
                    i++
                    if (i >= lines.size) break

                    val imageUrlLine = lines[i]
                    val imageUrlParts = imageUrlLine.split("#xywh=")
                    if (imageUrlParts.size == 2) {
                        val relativeImageUrl = imageUrlParts[0]
                        val fullImageUrl = if (relativeImageUrl.startsWith("http")) {
                            relativeImageUrl
                        } else {
                            URL(URL(baseUrl), relativeImageUrl).toString()
                        }
                        val xywhParts = imageUrlParts[1].split(",").mapNotNull { it.toIntOrNull() }
                        if (xywhParts.size == 4) {
                            cues.add(
                                ThumbnailCue(
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

/**
 * Parses a VTT file content for standard text captions.
 * @param vttContent The raw string content of the VTT file.
 * @return A list of [CaptionCue] objects.
 */
suspend fun parseCaptionCues(vttContent: String): List<CaptionCue> =
    withContext(Dispatchers.Default) {
        val cues = mutableListOf<CaptionCue>()
        val lines = vttContent.lines()
        var i = 0
        while (i < lines.size) {
            if (lines[i].contains("-->")) {
                val timeParts = lines[i].split("-->").map { it.trim() }
                if (timeParts.size == 2) {
                    try {
                        val startTime = parseTimestamp(timeParts[0])
                        val endTime = parseTimestamp(timeParts[1].split(" ")[0])

                        val textLines = mutableListOf<String>()
                        i++
                        while (i < lines.size && lines[i].isNotBlank()) {
                            textLines.add(lines[i])
                            i++
                        }
                        if (textLines.isNotEmpty()) {
                            cues.add(CaptionCue(startTime, endTime, textLines.joinToString("\n")))
                        }
                    } catch (_: Exception) {
                    }
                }
            }
            i++
        }
        cues
    }


/**
 * Finds the active thumbnail cue for a given media position.
 */
fun findThumbnailCueForPosition(position: Long, cues: List<ThumbnailCue>): ThumbnailCue? {
    return cues.find { position >= it.startTime && position < it.endTime }
}

/**
 * Finds all active caption cues for a given media position.
 * This now returns a List to support overlapping cues.
 */
fun findActiveCaptionCues(position: Long, cues: List<CaptionCue>): List<CaptionCue> {
    return cues.filter { position >= it.startTime && position < it.endTime }
}

/**
 * A Coil transformation for cropping a Bitmap to a specific rectangle.
 */
class CropTransformation(private val rect: Rect) : Transformation {
    override val cacheKey: String get() = "CropTransformation(rect=$rect)"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return Bitmap.createBitmap(input, rect.left, rect.top, rect.width(), rect.height())
    }
}