package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminoverse.animevibe.utils.media.CaptionCue
import java.util.regex.Pattern

/**
 * A custom view to render a list of subtitles with advanced placement logic.
 *
 * If more than one cue is active, it places the first cue at the top of the screen
 * and the remaining cues at the bottom. Otherwise, it places all cues at the bottom.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param cues The List of [CaptionCue]s to display. If empty or null, nothing is rendered.
 * @param isLandscape Whether the player is in landscape mode, to adjust font size.
 * @param isPipMode Whether the player is in Picture-in-Picture mode, for the smallest font size.
 */
@Composable
fun CustomSubtitleView(
    modifier: Modifier = Modifier,
    cues: List<CaptionCue>?,
    isLandscape: Boolean,
    isPipMode: Boolean
) {
    if (cues.isNullOrEmpty()) return

    Box(modifier = modifier) {
        val hasMultipleCues = cues.size > 1
        val topCue = if (hasMultipleCues) cues.first() else null
        val bottomCues = if (hasMultipleCues) cues.drop(1) else cues

        if (topCue != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (isPipMode) 4.dp else 16.dp)
            ) {
                SubtitleText(
                    text = topCue.text,
                    isLandscape = isLandscape,
                    isPipMode = isPipMode
                )
            }
        }

        if (bottomCues.isNotEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                bottomCues.forEach { cue ->
                    SubtitleText(
                        text = cue.text,
                        isLandscape = isLandscape,
                        isPipMode = isPipMode
                    )
                }
            }
        }
    }
}

/**
 * Renders a single subtitle text with a white fill and black outline effect.
 * Font size is adjusted based on the screen orientation and mode.
 *
 * @param text The raw text content of the subtitle cue.
 * @param isLandscape Whether the player is in landscape mode.
 * @param isPipMode Whether the player is in Picture-in-Picture mode.
 */
@Composable
private fun SubtitleText(
    text: String,
    isLandscape: Boolean,
    isPipMode: Boolean
) {
    val annotatedString = rememberVttAnnotatedString(text = text)

    val fontSize = when {
        isPipMode -> 10.sp
        isLandscape -> 16.sp
        else -> 12.sp
    }

    val lineHeight = fontSize * 1.3f

    val dropShadow = Shadow(
        color = Color.Black.copy(alpha = 0.75f),
        offset = androidx.compose.ui.geometry.Offset(x = 2f, y = 2f),
        blurRadius = 4f
    )

    Text(
        text = annotatedString,
        textAlign = TextAlign.Center,
        style = TextStyle(
            color = Color.White,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = FontWeight.Bold,
            shadow = dropShadow
        )
    )
}

/**
 * A memoized function that parses a VTT string with simple HTML tags (<b>, <i>)
 * and converts it into an [AnnotatedString] for Compose.
 *
 * @param text The raw text from the VTT cue.
 * @return An [AnnotatedString] with appropriate styling applied.
 */
@Composable
private fun rememberVttAnnotatedString(text: String): AnnotatedString {
    return remember(text) {
        buildAnnotatedString {
            val cleanedText = text.replace(Regex("<c\\..*?>|</c>"), "")
                .replace(Regex("<v.*?>|</v>"), "")

            val pattern = Pattern.compile("</?[bi]>")
            val matcher = pattern.matcher(cleanedText)

            val styles = mutableListOf<SpanStyle>()
            var lastIndex = 0

            while (matcher.find()) {
                val startIndex = matcher.start()
                val endIndex = matcher.end()
                val tag = matcher.group(0)

                if (startIndex > lastIndex) {
                    pushStyle(styles.lastOrNull() ?: SpanStyle())
                    append(cleanedText.substring(lastIndex, startIndex))
                    pop()
                }

                if (tag == "<b>") {
                    styles.add(SpanStyle(fontWeight = FontWeight.ExtraBold))
                } else if (tag == "<i>") {
                    styles.add(SpanStyle(fontStyle = FontStyle.Italic))
                } else if (tag == "</b>" || tag == "</i>") {
                    if (styles.isNotEmpty()) {
                        styles.removeAt(styles.lastIndex)
                    }
                }
                lastIndex = endIndex
            }

            if (lastIndex < cleanedText.length) {
                pushStyle(styles.lastOrNull() ?: SpanStyle())
                append(cleanedText.substring(lastIndex))
                pop()
            }
        }
    }
}