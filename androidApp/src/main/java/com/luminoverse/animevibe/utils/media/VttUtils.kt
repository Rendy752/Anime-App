package com.luminoverse.animevibe.utils.media

import android.graphics.Bitmap
import android.graphics.Rect
import coil.size.Size
import coil.transform.Transformation
import com.luminoverse.animevibe.utils.TimeUtils.parseTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class VttCue(
    val startTime: Long,
    val endTime: Long,
    val imageUrl: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

suspend fun parseThumbnailVtt(vttContent: String, baseUrl: String): List<VttCue> =
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
                            if (relativeImageUrl.startsWith("http://")
                                || relativeImageUrl.startsWith("https://")
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