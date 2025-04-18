package com.example.animeapp.ui.animeRecommendations

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import com.example.animeapp.R
import com.example.animeapp.models.NetworkStatus
import com.example.animeapp.ui.animeRecommendations.recommendations.RecommendationItem
import com.example.animeapp.ui.animeRecommendations.recommendations.RecommendationItemSkeleton
import com.example.animeapp.ui.common_ui.ErrorMessage
import com.example.animeapp.utils.NetworkStateMonitor
import com.example.animeapp.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeRecommendationsScreen(navController: NavController) {
    val viewModel: AnimeRecommendationsViewModel = hiltViewModel()

    val animeRecommendationsState by viewModel.animeRecommendations.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val state = rememberPullToRefreshState()

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val networkStateMonitor = remember { NetworkStateMonitor(context) }
    var networkStatus by remember { mutableStateOf(networkStateMonitor.networkStatus.value) }
    var isConnected by remember { mutableStateOf(networkStateMonitor.isConnected.value != false) }
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    DisposableEffect(Unit) {
        networkStateMonitor.startMonitoring(context)
        val networkObserver = Observer<NetworkStatus> {
            networkStatus = it
        }
        val connectionObserver = Observer<Boolean> {
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
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.title_recommendation),
                            modifier = Modifier.padding(end = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
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
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    thickness = 2.dp
                )
            }
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.getAnimeRecommendations() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = state,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    isRefreshing = isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primary,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = state
                )
            },
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (!isConnected) ErrorMessage(message = stringResource(R.string.no_internet_connection))

                when (animeRecommendationsState) {
                    is Resource.Loading -> {
                        if (!isLandscape) repeat(3) { RecommendationItemSkeleton() }
                        else {
                            Row(modifier = Modifier.fillMaxSize()) {
                                repeat(2) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        repeat(2) { RecommendationItemSkeleton() }
                                    }
                                }
                            }
                        }
                    }

                    is Resource.Success -> {
                        val animeRecommendations =
                            (animeRecommendationsState as Resource.Success).data.data
                        if (!isLandscape) {
                            LazyColumn {
                                items(animeRecommendations) {
                                    RecommendationItem(
                                        recommendation = it,
                                        onItemClick = { anime -> navController.navigate("animeDetail/${anime.title}/${anime.mal_id}") }
                                    )
                                }
                            }
                        } else {
                            val listSize = animeRecommendations.size
                            val itemsPerColumn = (listSize + 1) / 2
                            Row(modifier = Modifier.fillMaxSize()) {
                                repeat(2) { columnIndex ->
                                    val startIndex = columnIndex * itemsPerColumn
                                    val endIndex = minOf(startIndex + itemsPerColumn, listSize)
                                    val columnItems =
                                        animeRecommendations.subList(startIndex, endIndex)
                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        items(columnItems) {
                                            RecommendationItem(
                                                recommendation = it,
                                                onItemClick = { anime -> navController.navigate("animeDetail/${anime.title}/${anime.mal_id}") }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is Resource.Error -> {
                        if (isConnected) ErrorMessage(stringResource(R.string.error_loading_data))
                        else ErrorMessage(stringResource(R.string.no_internet_connection))
                    }
                }
            }
        }
    }
}