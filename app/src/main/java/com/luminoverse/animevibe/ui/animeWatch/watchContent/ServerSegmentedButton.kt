package com.luminoverse.animevibe.ui.animeWatch.watchContent

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.Server
import com.luminoverse.animevibe.ui.common.SkeletonBox
import com.luminoverse.animevibe.utils.watch.WatchUtils.getServerCategoryIcon

@Composable
fun ServerSegmentedButton(
    type: String,
    servers: List<Server>,
    onServerSelected: (EpisodeSourcesQuery) -> Unit,
    episodeId: String,
    episodeSourcesQuery: EpisodeSourcesQuery,
    errorSourceQueryList: List<EpisodeSourcesQuery>,
    modifier: Modifier = Modifier
) {
    if (servers.isEmpty()) return
    val selectedIndex: MutableState<Int> = remember { mutableIntStateOf(-1) }
    val serverMap = mapOf(
        "vidstreaming" to "vidsrc"
    )

    LaunchedEffect(episodeSourcesQuery) {
        val compareServer =
            serverMap.getOrDefault(episodeSourcesQuery.server, episodeSourcesQuery.server)
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
                enabled = index != selectedIndex.value || (errorSourceQueryList.any { errorQuery ->
                    errorQuery.category == type &&
                            serverMap.getOrDefault(
                                errorQuery.server, errorQuery.server
                            ) == server.serverName && episodeId == errorQuery.id
                }),
                label = {
                    Text(server.serverName)
                },
                icon = { getServerCategoryIcon(type)?.invoke() }
            )
        }
    }
}

@Preview
@Composable
fun ServerSegmentedButtonSkeleton(serversCount: Int = 3) {
    SingleChoiceSegmentedButtonRow {
        repeat(serversCount) { index ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = serversCount
                ),
                onClick = { },
                selected = false,
                enabled = false,
                label = {
                    SkeletonBox(width = 60.dp, height = 16.dp)
                },
                icon = { SkeletonBox(width = 24.dp, height = 24.dp) }
            )
        }
    }
}