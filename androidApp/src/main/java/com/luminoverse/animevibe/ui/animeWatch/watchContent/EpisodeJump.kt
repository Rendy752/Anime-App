package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.ui.common.CircularLoadingIndicator
import com.luminoverse.animevibe.ui.common.SearchView
import com.luminoverse.animevibe.ui.common.SearchViewSkeleton
import com.luminoverse.animevibe.utils.Debounce
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeJump(
    episodes: List<Episode>,
    episodeJumpNumber: Int?,
    setEpisodeJumpNumber: (Int) -> Unit,
    gridState: LazyGridState
) {
    val scope = rememberCoroutineScope()
    val totalEpisodes = episodes.size
    var textValue by remember {
        mutableStateOf(episodeJumpNumber?.toString()?.takeIf { it != "0" } ?: "")
    }

    LaunchedEffect(episodeJumpNumber) {
        textValue = episodeJumpNumber?.toString()?.takeIf { it != "0" } ?: ""
    }

    val debounce = remember {
        Debounce(scope, 1000L) { query ->
            val intValue = query.toIntOrNull()
            if (intValue != null && intValue >= 1 && intValue <= totalEpisodes) {
                val index = episodes.indexOfFirst { it.episode_no == intValue }
                if (index != -1) {
                    scope.launch {
                        val indexDifference = abs(gridState.firstVisibleItemIndex - index)
                        if (indexDifference < 50) gridState.animateScrollToItem(index)
                        else gridState.scrollToItem(index)
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(0.5f)
                .align(Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Episodes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                totalEpisodes.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }

        SearchView(
            query = textValue,
            onQueryChange = { newText ->
                val filteredText = newText.filter { it.isDigit() }
                val newIntValue = filteredText.toIntOrNull()

                if (newIntValue == null || newIntValue <= totalEpisodes) {
                    textValue = filteredText
                    setEpisodeJumpNumber(newIntValue ?: 0)
                    if (newIntValue != null && newIntValue > 0) {
                        debounce.query(newIntValue.toString())
                    }
                } else {
                    textValue = totalEpisodes.toString()
                    setEpisodeJumpNumber(totalEpisodes)
                    debounce.query(totalEpisodes.toString())
                }
            },
            keyboardType = KeyboardType.Number,
            placeholder = "Jump to Episode",
            modifier = Modifier.weight(0.5f)
        )
    }
}

@Composable
fun EpisodeJumpSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(0.5f)
                .align(Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { CircularLoadingIndicator(size = 24, strokeWidth = 2f) }
        SearchViewSkeleton(modifier = Modifier.weight(0.5f))
    }
}