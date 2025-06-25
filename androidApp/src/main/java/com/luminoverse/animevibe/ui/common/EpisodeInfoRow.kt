package com.luminoverse.animevibe.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.luminoverse.animevibe.ui.theme.dubColor
import com.luminoverse.animevibe.ui.theme.epsColor
import com.luminoverse.animevibe.ui.theme.subColor
import com.luminoverse.animevibe.utils.watch.WatchUtils.getServerCategoryIcon

@Composable
fun EpisodeInfoRow(
    subCount: Int?,
    dubCount: Int?,
    epsCount: Int?,
    modifier: Modifier = Modifier,
) {
    val counts = listOf(subCount, dubCount, epsCount)
    val colors = listOf(subColor, dubColor, epsColor)
    val icons = listOf(
        getServerCategoryIcon("sub"),
        getServerCategoryIcon("dub"),
        getServerCategoryIcon("raw")
    )

    val nonNullCounts = counts.filterNotNull()
    if (nonNullCounts.isNotEmpty()) {
        Row(modifier = modifier) {
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

@Preview
@Composable
fun EpisodeInfoRowSkeleton(modifier: Modifier = Modifier) {
    val colors = listOf(subColor, dubColor, epsColor)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
    ) {
        repeat(3) {
            EpisodeInfoItemSkeleton(
                isFirst = it == 0,
                isLast = it == 2,
                hasRight = it < 2,
                color = colors[it]
            )
        }
    }
}