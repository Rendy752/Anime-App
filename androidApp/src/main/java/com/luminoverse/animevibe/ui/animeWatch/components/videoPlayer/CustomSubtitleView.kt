package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
 * @param modifier The modifier to be applied to the layout. For this composable to correctly
 * align, the modifier passed from the parent should allow it to expand,
 * e.g., by using `Modifier.fillMaxSize()`.
 * @param cues The List of [CaptionCue]s to display. If empty or null, nothing is rendered.
 */
@Composable
fun CustomSubtitleView(
    modifier: Modifier = Modifier,
    cues: List<CaptionCue>?
) {
    if (cues.isNullOrEmpty()) return

    Box(modifier = modifier) {
        val hasMultipleCues = cues.size > 1
        val topCue = if (hasMultipleCues) cues.first() else null
        val bottomCues = if (hasMultipleCues) cues.drop(1) else cues

        if (topCue != null) {
            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                SubtitleText(text = topCue.text)
            }
        }

        if (bottomCues.isNotEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                bottomCues.forEach { cue ->
                    SubtitleText(text = cue.text)
                }
            }
        }
    }
}

/**
 * Renders a single subtitle text with a white fill and black outline effect.
 * @param text The raw text content of the subtitle cue.
 */
@Composable
private fun SubtitleText(text: String) {
    val annotatedString = rememberVttAnnotatedString(text = text)

    Box(contentAlignment = Alignment.Center) {
        Text(
            text = annotatedString,
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(x = 1.dp, y = 1.dp)
        )
        Text(
            text = annotatedString,
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(x = (-1).dp, y = 1.dp)
        )
        Text(
            text = annotatedString,
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(x = 1.dp, y = (-1).dp)
        )
        Text(
            text = annotatedString,
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(x = (-1).dp, y = (-1).dp)
        )

        Text(
            text = annotatedString,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
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