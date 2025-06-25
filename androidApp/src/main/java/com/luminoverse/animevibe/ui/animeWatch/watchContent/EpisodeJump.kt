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
import com.luminoverse.animevibe.ui.common.SearchView
import com.luminoverse.animevibe.utils.Debounce
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeJump(
    episodes: List<Episode>,
    gridState: LazyGridState
) {
    val scope = rememberCoroutineScope()
    var episodeNumberInput by remember { mutableStateOf("") }
    val totalEpisodes = episodes.size

    val debounce = remember {
        Debounce(scope, 1000L) { filteredQuery ->
            val intValue = filteredQuery.toIntOrNull()
            if (intValue != null && intValue >= 1 && intValue <= totalEpisodes) {
                val index = episodes.indexOfFirst { it.episode_no == intValue }
                if (index != -1) {
                    scope.launch {
                        if (abs(gridState.firstVisibleItemIndex - index) < 20) {
                            gridState.animateScrollToItem(index)
                        } else {
                            gridState.scrollToItem(index)
                        }
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                totalEpisodes.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        SearchView(
            query = episodeNumberInput,
            onQueryChange = { newText ->
                val filteredText = newText.filter { it.isDigit() && it != '0' }
                val newIntValue = filteredText.toIntOrNull()
                episodeNumberInput = if (newIntValue == null || newIntValue <= totalEpisodes) {
                    filteredText
                } else {
                    totalEpisodes.toString()
                }
                debounce.query(episodeNumberInput)
            },
            keyboardType = KeyboardType.Number,
            placeholder = "Jump to Episode",
            modifier = Modifier.weight(0.5f)
        )
    }
}