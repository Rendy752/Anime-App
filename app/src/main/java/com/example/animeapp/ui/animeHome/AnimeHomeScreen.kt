package com.example.animeapp.ui.animeHome

import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.navigation.NavHostController
import com.example.animeapp.R
import com.example.animeapp.models.NetworkStatus
import com.example.animeapp.ui.animeHome.components.ContinueWatchingPopup
import com.example.animeapp.ui.animeRecommendations.recommendations.RecommendationItemSkeleton
import com.example.animeapp.ui.common_ui.ErrorMessage
import com.example.animeapp.ui.main.BottomScreen
import com.example.animeapp.utils.NetworkStateMonitor
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.basicContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeHomeScreen(currentRoute: String?, navController: NavHostController) {
    val viewModel: HomeViewModel = hiltViewModel()

    val watchRecentEpisode by viewModel.watchRecentEpisode.collectAsState()
    val continueWatchingEpisode by viewModel.continueWatchingEpisode.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val state = rememberPullToRefreshState()

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val networkStateMonitor = remember { NetworkStateMonitor(context) }
    var networkStatus by remember { mutableStateOf(networkStateMonitor.networkStatus.value) }
    var isConnected by remember { mutableStateOf(networkStateMonitor.isConnected.value != false) }
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isShowPopup by remember { mutableStateOf(false) }
    var isMinimized by remember { mutableStateOf(false) }
    var episodeData by remember {
        mutableStateOf<com.example.animeapp.models.EpisodeDetailComplement?>(
            null
        )
    }

    LaunchedEffect(currentRoute) {
        viewModel.fetchContinueWatchingEpisode()
    }

    LaunchedEffect(continueWatchingEpisode) {
        if (continueWatchingEpisode is Resource.Success) {
            episodeData = continueWatchingEpisode.data
            isShowPopup = episodeData != null
            if (isShowPopup) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isMinimized) {
                        isShowPopup = false
                    }
                }, 10000)
            }
        } else {
            isShowPopup = false
        }
    }

    DisposableEffect(Unit) {
        networkStateMonitor.startMonitoring(context)
        val networkObserver = Observer<NetworkStatus> {
            networkStatus = it
        }
        val connectionObserver = Observer<Boolean> {
            isConnected = it
            if (isConnected && watchRecentEpisode is Resource.Error) {
                viewModel.getWatchRecentEpisode()
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
                            text = BottomScreen.Recommendations.label,
                            modifier = Modifier.padding(end = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
            onRefresh = { viewModel.getWatchRecentEpisode() },
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

                when (watchRecentEpisode) {
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
                        watchRecentEpisode.data?.data?.let { watchRecentEpisode ->
                            if (!isLandscape) {
                                LazyColumn {
                                    items(watchRecentEpisode) {
                                        Column(modifier = Modifier.basicContainer(onItemClick = {
                                            navController.navigate("animeDetail/${it.entry.title}/${it.entry.mal_id}")
                                        })) {
                                            Text(it.entry.title)
                                            Text(it.episodes[0].title)
                                        }
                                    }
                                }
                            } else {
                                val listSize = watchRecentEpisode.size
                                val itemsPerColumn = (listSize + 1) / 2
                                Row(modifier = Modifier.fillMaxSize()) {
                                    repeat(2) { columnIndex ->
                                        val startIndex = columnIndex * itemsPerColumn
                                        val endIndex = minOf(startIndex + itemsPerColumn, listSize)
                                        val columnItems =
                                            watchRecentEpisode.subList(startIndex, endIndex)
                                        LazyColumn(modifier = Modifier.weight(1f)) {
                                            items(columnItems) {
                                                Column(
                                                    modifier = Modifier.basicContainer(
                                                        onItemClick = {
                                                            navController.navigate("animeDetail/${it.entry.title}/${it.entry.mal_id}")
                                                        })
                                                ) {
                                                    Text(it.entry.title)
                                                    Text(it.episodes[0].title)
                                                }
                                            }
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
                ContinueWatchingPopup(
                    isShowPopup = isShowPopup,
                    episode = episodeData,
                    onMinimize = {
                        isShowPopup = false
                        isMinimized = true
                    },
                    onRestore = {
                        isShowPopup = true
                        isMinimized = false
                    },
                    isMinimized = isMinimized
                )
            }
        }
    }
}