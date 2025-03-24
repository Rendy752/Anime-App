package com.example.animeapp.ui.animeWatch.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSegmentedButton(
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("Watch", "Details")

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = { onSelectedIndexChange(index) },
                selected = index == selectedIndex,
                label = {
                    when (label) {
                        "Watch" -> Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = "Watch"
                        )

                        "Details" -> Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details"
                        )

                        else -> Text(label)
                    }
                }
            )
        }
    }
}