package com.example.animeapp.ui.animeWatch.watchContent

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
import com.example.animeapp.models.Episode
import com.example.animeapp.ui.common_ui.SearchView
import com.example.animeapp.utils.Debounce
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeJump(
    episodes: List<Episode>,
    gridState: LazyGridState
) {
    val scope = rememberCoroutineScope()
    var episodeNumberInput by remember { mutableStateOf("") }
    val debounce = remember(episodeNumberInput) {
        Debounce(scope, 500L) { newQuery ->
            val filteredValue = newQuery.filter { it.isDigit() }
            val intValue = filteredValue.toIntOrNull()
            if (intValue == null || (intValue >= 1 && intValue <= episodes.size)) {
                episodeNumberInput = filteredValue
                intValue?.let { episodeNo ->
                    val index = episodes.indexOfFirst { it.episodeNo == episodeNo }
                    if (index != -1) {
                        scope.launch {
                            gridState.animateScrollToItem(index)
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
                episodes.size.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        SearchView(
            query = episodeNumberInput,
            onQueryChange = {
                episodeNumberInput = it
                debounce.query(it)
            },
            keyboardType = KeyboardType.Number,
            placeholder = "Jump to Episode",
            modifier = Modifier.weight(0.5f)
        )
    }
}