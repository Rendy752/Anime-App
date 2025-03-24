package com.example.animeapp.ui.animeWatch.ui

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.NetworkStatus
import com.example.animeapp.ui.animeWatch.AnimeWatchViewModel
import com.example.animeapp.ui.animeWatch.components.AnimeWatchTopBar
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.utils.NetworkStateMonitor
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ScreenOffReceiver
import com.example.animeapp.utils.ScreenOnReceiver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeWatchScreen(
    animeDetail: AnimeDetail,
    episodeId: String,
    episodesList: List<Episode>,
    defaultEpisode: EpisodeDetailComplement,
    navController: NavController,
    isPipMode: Boolean,
    onEnterPipMode: () -> Unit
) {
    val viewModel: AnimeWatchViewModel = hiltViewModel()

    // ViewModel Data
    val episodeDetailComplement by viewModel.episodeDetailComplement.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val episodeSourcesQuery by viewModel.episodeSourcesQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // UI State
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    val state = rememberPullToRefreshState()
    var selectedContentIndex by remember { mutableIntStateOf(0) }

    // Device State
    var isScreenOn by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val screenOffReceiver = remember { ScreenOffReceiver { isScreenOn = false } }
    val screenOnReceiver = remember { ScreenOnReceiver { isScreenOn = true } }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Network State
    val networkStateMonitor = remember { NetworkStateMonitor(context) }
    var networkStatus by remember { mutableStateOf(networkStateMonitor.networkStatus.value) }
    var isConnected by remember { mutableStateOf(networkStateMonitor.isConnected.value != false) }

    BackHandler(enabled = isFullscreen) {
        if (isFullscreen) {
            isFullscreen = false
            (context as? Activity)?.window?.let { window ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        viewModel.setInitialState(animeDetail, episodesList, defaultEpisode)

        networkStateMonitor.startMonitoring(context)
        val networkObserver = Observer<NetworkStatus> {
            networkStatus = it
        }
        val connectionObserver = Observer<Boolean> { isConnected = it }
        networkStateMonitor.networkStatus.observeForever(networkObserver)
        networkStateMonitor.isConnected.observeForever(connectionObserver)
        context.registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        context.registerReceiver(screenOnReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))

        onDispose {
            context.unregisterReceiver(screenOffReceiver)
            context.unregisterReceiver(screenOnReceiver)
            networkStateMonitor.stopMonitoring()
            networkStateMonitor.networkStatus.removeObserver(networkObserver)
            networkStateMonitor.isConnected.removeObserver(connectionObserver)
        }
    }

    LaunchedEffect(Unit) {
        episodeSourcesQuery?.let { query ->
            viewModel.handleSelectedEpisodeServer(query.copy(id = episodeId))
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
            errorMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isPipMode && !isFullscreen) AnimeWatchTopBar(
                animeDetail,
                isLandscape,
                networkStatus,
                selectedContentIndex,
                { selectedContentIndex = it },
                navController
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                episodeSourcesQuery?.let { query ->
                    viewModel.handleSelectedEpisodeServer(query, true)
                }
            },
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
            val videoSize = if (isLandscape) Modifier.fillMaxSize()
            else if (!isPipMode && !isFullscreen) Modifier.height(250.dp)
            else Modifier.fillMaxSize()

            Column(modifier = Modifier.fillMaxSize()) {
                when (episodeDetailComplement) {
                    is Resource.Loading -> SkeletonBox(modifier = Modifier.then(videoSize))

                    is Resource.Success -> {
                        episodeDetailComplement.data?.let { episodeDetailComplement ->
                            episodes?.let { episodeList ->
                                episodeSourcesQuery?.let { query ->
                                    val videoPlayerModifier = Modifier
                                        .then(if (isLandscape) Modifier.weight(0.5f) else Modifier.fillMaxWidth())
                                        .then(videoSize)

                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        VideoPlayerSection(
                                            episodeDetailComplement = episodeDetailComplement,
                                            episodes = episodeList,
                                            episodeSourcesQuery = query,
                                            handleSelectedEpisodeServer = {
                                                viewModel.handleSelectedEpisodeServer(
                                                    it
                                                )
                                            },
                                            isPipMode = isPipMode,
                                            onEnterPipMode = onEnterPipMode,
                                            isFullscreen = isFullscreen,
                                            onFullscreenChange = { isFullscreen = it },
                                            isScreenOn = isScreenOn,
                                            isLandscape = isLandscape,
                                            onPlayerError = { message -> errorMessage = message },
                                            modifier = videoPlayerModifier,
                                            videoSize = videoSize
                                        )

                                        if (isLandscape && !isPipMode && !isFullscreen) {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .weight(0.5f)
                                                    .padding(8.dp),
                                                state = scrollState
                                            ) {
                                                item {
                                                    if (selectedContentIndex == 0) {
                                                        episodes?.let { episodes ->
                                                            WatchContentSection(
                                                                animeDetail,
                                                                episodeDetailComplement,
                                                                episodes,
                                                                episodeSourcesQuery
                                                            ) {
                                                                viewModel.handleSelectedEpisodeServer(
                                                                    it
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        InfoContentSection(animeDetail)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (!isLandscape && !isPipMode && !isFullscreen) {
                                        LazyColumn(
                                            modifier = Modifier.padding(8.dp),
                                            state = scrollState
                                        ) {
                                            item {
                                                episodes?.let { episodes ->
                                                    WatchContentSection(
                                                        animeDetail,
                                                        episodeDetailComplement,
                                                        episodes,
                                                        episodeSourcesQuery
                                                    ) { viewModel.handleSelectedEpisodeServer(it) }
                                                }
                                                InfoContentSection(animeDetail)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is Resource.Error -> {
                        episodeSourcesQuery?.let { query ->
                            viewModel.handleSelectedEpisodeServer(query.copy(id = episodeId))
                        }
                    }
                }
            }
        }
    }
}