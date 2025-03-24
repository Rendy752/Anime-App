package com.example.animeapp.ui.animeWatch.components

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.animeapp.models.Server

@Composable
fun ServerSegmentedButton(
    servers: List<Server>,
    onServerSelected: (Server) -> Unit,
    modifier: Modifier = Modifier
) {
    if (servers.isEmpty()) return

    val selectedIndex: MutableState<Int> = remember { mutableIntStateOf(0) }
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        servers.forEachIndexed { index, server ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = servers.size
                ),
                onClick = {
                    selectedIndex.value = index
                    onServerSelected(server)
                },
                selected = index == selectedIndex.value,
                label = {
                    Text(server.serverName)
                }
            )
        }
    }
}