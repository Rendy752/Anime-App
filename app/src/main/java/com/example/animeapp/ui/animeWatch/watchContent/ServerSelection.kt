package com.example.animeapp.ui.animeWatch.watchContent

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodeSourcesQuery

@Composable
fun ServerSelection(
    episodeSourcesQuery: EpisodeSourcesQuery?,
    servers: EpisodeServersResponse,
    onServerSelected: (EpisodeSourcesQuery) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        episodeSourcesQuery?.let { query ->
            listOf(
                "sub" to servers.sub,
                "dub" to servers.dub,
                "raw" to servers.raw
            ).forEach { (type, servers) ->
                if (servers.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ServerSegmentedButton(
                            type,
                            servers,
                            onServerSelected = onServerSelected,
                            episodeSourcesQuery = query
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ServerSelectionSkeleton() {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("sub", "dub", "raw").forEach { _ ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ServerSegmentedButtonSkeleton()
            }
        }
    }
}