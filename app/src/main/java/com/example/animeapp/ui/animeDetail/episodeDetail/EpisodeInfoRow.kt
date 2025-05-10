package com.example.animeapp.ui.animeDetail.episodeDetail

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.animeapp.ui.theme.dubColor
import com.example.animeapp.ui.theme.epsColor
import com.example.animeapp.ui.theme.subColor
import com.example.animeapp.utils.WatchUtils.getServerCategoryIcon

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
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
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

@Preview
@Composable
fun EpisodeInfoRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        repeat(3) {
            EpisodeInfoItemSkeleton(
                isFirst = it == 0,
                isLast = it == 2,
                hasRight = it < 2
            )
        }
    }
}