package com.example.animeapp.ui.animeRecommendations.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.animeapp.R
import com.example.animeapp.models.NetworkStatus
import com.example.animeapp.ui.animeRecommendations.components.AnimeRecommendationItem
import com.example.animeapp.ui.animeRecommendations.components.AnimeRecommendationItemSkeleton
import com.example.animeapp.ui.animeRecommendations.viewmodel.AnimeRecommendationsViewModel
import com.example.animeapp.utils.NetworkStateMonitor
import com.example.animeapp.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeRecommendationsScreen(navController: NavController) {
    val viewModel: AnimeRecommendationsViewModel = hiltViewModel()
    val animeRecommendationsState by viewModel.animeRecommendations.collectAsState()
    val context = LocalContext.current
    val networkStateMonitor = remember { NetworkStateMonitor(context) }
    var networkStatus by remember { mutableStateOf(networkStateMonitor.networkStatus.value) }
    var isConnected by remember { mutableStateOf(networkStateMonitor.isConnected.value != false) }

    DisposableEffect(Unit) {
        networkStateMonitor.startMonitoring(context)
        val networkObserver = androidx.lifecycle.Observer<NetworkStatus> {
            networkStatus = it
        }
        val connectionObserver = androidx.lifecycle.Observer<Boolean> {
            isConnected = it
            if (isConnected && animeRecommendationsState is Resource.Error) {
                viewModel.getAnimeRecommendations()
            }
        }
        networkStateMonitor.networkStatus.observeForever(networkObserver)
        networkStateMonitor.isConnected.observeForever(connectionObserver)
        onDispose {
            networkStateMonitor.stopMonitoring()
            networkStateMonitor.networkStatus.removeObserver(networkObserver)
            networkStateMonitor.isConnected.removeObserver(connectionObserver)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.title_recommendation)) },
                actions = {
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            if (animeRecommendationsState is Resource.Success && isConnected) {
                FloatingActionButton(onClick = { viewModel.getAnimeRecommendations() }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(id = R.string.refresh_button)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!isConnected) {
                Text(
                    text = stringResource(id = R.string.no_internet_connection),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            when (animeRecommendationsState) {
                is Resource.Loading -> {
                    repeat(3) { AnimeRecommendationItemSkeleton() }
                }

                is Resource.Success -> {
                    val animeRecommendations =
                        (animeRecommendationsState as Resource.Success).data?.data ?: emptyList()
                    LazyColumn {
                        items(animeRecommendations) { recommendation ->
                            AnimeRecommendationItem(
                                recommendation = recommendation,
                                onItemClick = { animeId -> navController.navigate("animeDetail/$animeId") }
                            )
                        }
                    }
                }

                is Resource.Error -> {
                    if (isConnected) {
                        Text(
                            text = stringResource(id = R.string.error_loading_data),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}