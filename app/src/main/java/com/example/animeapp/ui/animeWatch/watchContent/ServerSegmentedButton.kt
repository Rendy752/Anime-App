package com.example.animeapp.ui.animeWatch.watchContent

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.models.Server
import com.example.animeapp.utils.WatchUtils.getServerCategoryIcon

@Composable
fun ServerSegmentedButton(
    type: String,
    servers: List<Server>,
    onServerSelected: (EpisodeSourcesQuery) -> Unit,
    episodeSourcesQuery: EpisodeSourcesQuery,
    modifier: Modifier = Modifier
) {
    if (servers.isEmpty()) return
    val selectedIndex: MutableState<Int> = remember { mutableIntStateOf(-1) }

    LaunchedEffect(episodeSourcesQuery) {
        val compareServer =
            if (episodeSourcesQuery.server == "vidstreaming") "vidsrc" else episodeSourcesQuery.server
        val index = servers.indexOfFirst {
            it.serverName == compareServer && type == episodeSourcesQuery.category
        }
        if (index != -1) selectedIndex.value = index
        else selectedIndex.value = -1
    }
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        servers.forEachIndexed { index, server ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = servers.size
                ),
                colors = SegmentedButtonDefaults.colors(
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                    disabledActiveContainerColor = MaterialTheme.colorScheme.primary,
                    disabledActiveContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                onClick = {
                    selectedIndex.value = index
                    onServerSelected(
                        episodeSourcesQuery.copy(
                            server = server.serverName,
                            category = type
                        )
                    )
                },
                selected = index == selectedIndex.value,
                enabled = index != selectedIndex.value,
                label = {
                    Text(server.serverName)
                },
                icon = { getServerCategoryIcon(type)?.invoke() }
            )
        }
    }
}