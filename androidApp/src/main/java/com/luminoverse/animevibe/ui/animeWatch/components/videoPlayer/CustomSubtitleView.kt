package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminoverse.animevibe.ui.theme.SubtitleFontFamily
import com.luminoverse.animevibe.utils.media.CaptionCue
import java.util.regex.Pattern

/**
 * A custom view to render a list of subtitles with advanced placement logic.
 *
 * If more than one cue is active, it places the first cue at the top of the screen
 * and the remaining cues at the bottom. Each section has a constrained height,
 * and the text auto-sizes to fit within it.
 *
 * @param modifier The modifier to be applied to the layout.
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
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f),
                contentAlignment = Alignment.TopCenter
            ) { SubtitleText(text = topCue.text) }
        }

        if (bottomCues.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(if (hasMultipleCues) 0.4f else 0.25f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.Bottom)
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
 * The text auto-sizes to fill the parent container's bounds.
 *
 * @param text The raw text content of the subtitle cue.
 */
@Composable
private fun SubtitleText(text: String) {
    val annotatedString = rememberVttAnnotatedString(text = text)
    val shadowOffset = 1.dp

    val outlineStyle = TextStyle(
        textAlign = TextAlign.Center,
        fontFamily = SubtitleFontFamily,
        color = Color.Black,
        drawStyle = Stroke(
            miter = 10f,
            width = 5f,
            join = StrokeJoin.Round
        )
    )

    val textStyle = TextStyle(
        textAlign = TextAlign.Center,
        fontFamily = SubtitleFontFamily,
        color = Color.White
    )

    // Use uniform auto-sizing without a max font size for better fluid scaling.
    val textAutoSize = TextAutoSize.StepBased(
        minFontSize = 10.sp,
        maxFontSize = 24.sp
    )

    // This Box fills the constrained space from the parent. By removing the
    // contentAlignment, it will inherit the alignment from its parent.
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BasicText(
            text = annotatedString,
            style = outlineStyle,
            modifier = Modifier.offset(x = shadowOffset, y = shadowOffset),
            autoSize = textAutoSize
        )
        BasicText(
            text = annotatedString,
            style = outlineStyle,
            autoSize = textAutoSize
        )
        BasicText(
            text = annotatedString,
            style = textStyle,
            autoSize = textAutoSize
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
                .replace("\\h", "\u00A0")

            val pattern = Pattern.compile("</?[bi]>")
            val matcher = pattern.matcher(cleanedText)

            val styleStack = mutableListOf<SpanStyle>()
            var lastIndex = 0

            while (matcher.find()) {
                val startIndex = matcher.start()
                val endIndex = matcher.end()

                if (startIndex > lastIndex) {
                    val combinedStyle =
                        styleStack.fold(SpanStyle()) { acc, style -> acc.merge(style) }
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
}