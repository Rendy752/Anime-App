package com.example.animeapp.ui.animeWatch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.NetworkStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeWatchTopBar(
    animeDetail: AnimeDetail,
    isLandscape: Boolean,
    networkStatus: NetworkStatus?,
    selectedContentIndex: Int,
    onContentIndexChange: (Int) -> Unit,
    navController: NavController
) {
    Column {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            title = {
                Text(
                    animeDetail.title,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            },
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    networkStatus?.let {
                        Row {
                            Text(
                                text = it.label,
                                color = if (it.color == MaterialTheme.colorScheme.onError) MaterialTheme.colorScheme.onError
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = it.icon,
                                contentDescription = it.label,
                                tint = it.color
                            )
                        }
                    }
                    if (isLandscape) ContentSegmentedButton(
                        selectedIndex = selectedContentIndex,
                        onSelectedIndexChange = onContentIndexChange
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                titleContentColor = MaterialTheme.colorScheme.primary
            )
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceContainer,
            thickness = 2.dp
        )
    }
}