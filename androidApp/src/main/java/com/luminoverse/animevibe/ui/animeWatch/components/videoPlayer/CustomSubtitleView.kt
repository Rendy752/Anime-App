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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminoverse.animevibe.ui.theme.SubtitleFontFamily
import com.luminoverse.animevibe.utils.media.CaptionCue
import com.luminoverse.animevibe.utils.media.vttTextToAnnotatedString

/**
 * A helper function to count the number of lines in a given text block.
 * @param text The string to analyze.
 * @return The number of lines.
 */
private fun countLines(text: String): Int {
    return text.count { it == '\n' } + 1
}

/**
 * A custom view to render a list of subtitles with advanced placement logic.
 *
 * This view dynamically adjusts the vertical space for subtitles based on their
 * line count. It allocates a total screen height fraction for all cues and then
 * divides that space proportionally between top and bottom cues if both are present.
 * This prevents subtitles from becoming excessively large and ensures a balanced layout.
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

        val topLines = topCue?.let { countLines(it.text) } ?: 0
        val bottomLines = bottomCues.sumOf { countLines(it.text) }
        val totalLines = topLines + bottomLines

        if (totalLines == 0) return

        val fractionPerLine = 0.1f
        val minTotalFraction = 0.1f
        val maxTotalFraction = 0.4f
        val totalTargetFraction = (totalLines * fractionPerLine).coerceIn(minTotalFraction, maxTotalFraction)

        val topPortion = if (totalLines > 0) topLines.toFloat() / totalLines.toFloat() else 0f
        val bottomPortion = if (totalLines > 0) bottomLines.toFloat() / totalLines.toFloat() else 0f

        val topContainerFraction = totalTargetFraction * topPortion
        val bottomContainerFraction = totalTargetFraction * bottomPortion

        if (topCue != null && topContainerFraction > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(topContainerFraction),
                contentAlignment = Alignment.TopCenter
            ) { SubtitleText(text = topCue.text) }
        }

        if (bottomCues.isNotEmpty() && bottomContainerFraction > 0f) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(bottomContainerFraction),
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
    val annotatedString = vttTextToAnnotatedString(text)
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

    val textAutoSize = TextAutoSize.StepBased(
        minFontSize = 10.sp,
        maxFontSize = 20.sp
    )

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