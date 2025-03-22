package com.example.animeapp.ui.animeDetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.animeapp.ui.theme.dubColor
import com.example.animeapp.ui.theme.epsColor
import com.example.animeapp.ui.theme.subColor

@Composable
fun EpisodeInfoRow(
    subCount: Int?,
    dubCount: Int?,
    epsCount: Int?,
) {
    val counts = listOf(subCount, dubCount, epsCount)
    val colors = listOf(subColor, dubColor, epsColor)
    val icons = listOf(Icons.Default.Subtitles, Icons.Default.Mic, Icons.Default.LiveTv)

    val nonNullCounts = counts.filterNotNull()

    if (nonNullCounts.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            var firstNonNullIndex = -1
            var lastNonNullIndex = -1

            counts.forEachIndexed { index, count ->
                if (count != null) {
                    if (firstNonNullIndex == -1) {
                        firstNonNullIndex = index
                    }
                    lastNonNullIndex = index
                }
            }

            counts.forEachIndexed { index, count ->
                if (count != null) {
                    EpisodeInfoItem(
                        text = count.toString(),
                        color = colors[index],
                        icon = icons[index],
                        isFirst = index == firstNonNullIndex,
                        isLast = index == lastNonNullIndex,
                        hasRight = index < lastNonNullIndex,
                    )
                }
            }
        }
    }
}