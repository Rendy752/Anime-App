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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.NetworkStatus
import com.example.animeapp.ui.animeWatch.AnimeWatchViewModel
import com.example.animeapp.utils.NetworkStateMonitor
import com.example.animeapp.utils.ScreenOffReceiver
import com.example.animeapp.utils.ScreenOnReceiver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeWatchScreen(
    animeDetail: AnimeDetail,
    episodeId: String,
    episodes: List<Episode>,
    defaultEpisode: EpisodeDetailComplement,
    navController: NavController,
    isPipMode: Boolean,
    onEnterPipMode: () -> Unit
) {
    val viewModel: AnimeWatchViewModel = hiltViewModel()

    // ViewModel Data
    val episodeDetailComplement by viewModel.episodeDetailComplement.collectAsState()
    val episodeSourcesQuery by viewModel.episodeSourcesQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // UI State
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    val state = rememberPullToRefreshState()

    // Device State
    var isScreenOn by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val screenOffReceiver = remember { ScreenOffReceiver { isScreenOn = false } }
    val screenOnReceiver = remember { ScreenOnReceiver { isScreenOn = true } }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

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
        viewModel.setInitialState(animeDetail, episodes, defaultEpisode)

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

    LaunchedEffect(episodeSourcesQuery) {
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
            if (!isPipMode && !isFullscreen) {
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
                                animeDetail.title, maxLines = 1,
                                overflow = TextOverflow.Companion.Ellipsis
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
            }
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

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    PlayerSection(
                        episodeDetailComplement,
                        viewModel,
                        isPipMode,
                        onEnterPipMode,
                        isFullscreen,
                        { isFullscreen = it },
                        isScreenOn,
                        episodeId,
                        episodeSourcesQuery,
                        Modifier.weight(1f),
                        videoSize,
                        { message -> errorMessage = message })
                    if (!isPipMode && !isFullscreen) {
                        ContentSection(animeDetail, scrollState, Modifier.weight(1f))
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    PlayerSection(
                        episodeDetailComplement,
                        viewModel,
                        isPipMode,
                        onEnterPipMode,
                        isFullscreen,
                        { isFullscreen = it },
                        isScreenOn,
                        episodeId,
                        episodeSourcesQuery,
                        Modifier,
                        videoSize,
                        { message -> errorMessage = message })
                    if (!isPipMode && !isFullscreen) {
                        ContentSection(animeDetail, scrollState, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}