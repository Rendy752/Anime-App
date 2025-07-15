package com.luminoverse.animevibe.utils.media

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import coil.size.Size
import coil.transform.Transformation
import com.luminoverse.animevibe.utils.TimeUtils.parseTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.regex.Pattern

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
 * This version groups any cues that share the exact same timestamp into a single cue.
 * @param vttContent The raw string content of the VTT file.
 * @return A list of [CaptionCue] objects with unique timestamps.
 */
suspend fun parseCaptionCues(vttContent: String): List<CaptionCue> =
    withContext(Dispatchers.Default) {
        val groupedCues = mutableMapOf<Pair<Long, Long>, MutableList<String>>()
        val lines = vttContent.lines()
        var i = 0

        while (i < lines.size) {
            if (lines[i].contains("-->")) {
                val timeParts = lines[i].split("-->").map { it.trim() }
                if (timeParts.size == 2) {
                    try {
                        val startTime = parseTimestamp(timeParts[0])
                        val endTime = parseTimestamp(timeParts[1].split(" ")[0])
                        val timeKey = Pair(startTime, endTime)

                        val textLines = mutableListOf<String>()
                        i++
                        while (i < lines.size && lines[i].isNotBlank()) {
                            textLines.add(lines[i])
                            i++
                        }
                        if (textLines.isNotEmpty()) {
                            groupedCues.getOrPut(timeKey) { mutableListOf() }
                                .add(textLines.joinToString("\n"))
                        }
                    } catch (_: Exception) {
                    }
                }
            }
            i++
        }

        return@withContext groupedCues.map { (time, texts) ->
            CaptionCue(
                startTime = time.first,
                endTime = time.second,
                text = texts.joinToString("\n")
            )
        }.sortedBy { it.startTime }
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
 * Parses a VTT string with simple HTML tags (<b>, <i>)
 * and converts it into an [AnnotatedString] for Compose.
 *
 * @param text The raw text from the VTT cue.
 * @return An [AnnotatedString] with appropriate styling applied.
 */
fun vttTextToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        val cleanedText = text.replace(Regex("<c\\..*?>|</c>"), "")
            .replace(Regex("<v.*?>|</v>"), "")
            .replace("\\h", "\u00A0") // Replace horizontal tab with non-breaking space

        val pattern = Pattern.compile("</?[bi]>")
        val matcher = pattern.matcher(cleanedText)
        val styleStack = mutableListOf<SpanStyle>()
        var lastIndex = 0

        while (matcher.find()) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            if (startIndex > lastIndex) {
                val combinedStyle = styleStack.fold(SpanStyle()) { acc, style -> acc.merge(style) }
                pushStyle(combinedStyle)
                append(cleanedText.substring(lastIndex, startIndex))
                pop()
            }

            when (matcher.group(0)) {
                "<b>" -> styleStack.add(SpanStyle(fontWeight = FontWeight.Bold))
                "<i>" -> styleStack.add(SpanStyle(fontStyle = FontStyle.Italic))
                "</b>", "</i>" -> {
                    styleStack.removeLastOrNull()
                }
            }
            lastIndex = endIndex
        }

        if (lastIndex < cleanedText.length) {
            val combinedStyle = styleStack.fold(SpanStyle()) { acc, style -> acc.merge(style) }
            pushStyle(combinedStyle)
            append(cleanedText.substring(lastIndex))
            pop()
        }
    }
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