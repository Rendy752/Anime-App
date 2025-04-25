package com.example.animeapp.ui.animeWatch

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.animeapp.ui.animeWatch.components.AnimeWatchContent
import com.example.animeapp.ui.animeWatch.components.AnimeWatchTopBar
import com.example.animeapp.ui.main.MainState
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ScreenOffReceiver
import com.example.animeapp.utils.ScreenOnReceiver
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AnimeWatchScreen(
    malId: Int = 0,
    episodeId: String = "",
    navController: NavHostController = rememberNavController(),
    mainState: MainState = MainState(),
    isPipMode: Boolean = false,
    onEnterPipMode: () -> Unit = {},
) {
    val viewModel: AnimeWatchViewModel = hiltViewModel()

    // ViewModel Data
    val animeDetail by viewModel.animeDetail.collectAsStateWithLifecycle()
    val animeDetailComplement by viewModel.animeDetailComplement.collectAsStateWithLifecycle()
    val episodeDetailComplement by viewModel.episodeDetailComplement.collectAsStateWithLifecycle()
    val episodeSourcesQuery by viewModel.episodeSourcesQuery.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // UI State
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isFavorite = remember { mutableStateOf(false) }
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

    val onBackPress: () -> Unit = {
        if (isFullscreen) {
            isFullscreen = false
            (context as? Activity)?.window?.let { window ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }
            }
        } else {
            navController.popBackStack()
        }
    }

    BackHandler {
        onBackPress()
    }

    LaunchedEffect(Unit) {
        scope.launch {
            viewModel.setInitialState(malId, episodeId)
        }
    }

    LaunchedEffect(episodeDetailComplement) {
        if (episodeDetailComplement is Resource.Success) {
            isFavorite.value = episodeDetailComplement.data?.isFavorite == true
        }
        if (episodeDetailComplement is Resource.Error) {
            snackbarHostState.showSnackbar(
                "Failed to fetch episode sources, returning to the previous episode. Check your internet connection or try again later after 1 hour."
            )
            viewModel.handleSelectedEpisodeServer(episodeSourcesQuery)
        }
    }

    DisposableEffect(Unit) {
        context.registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        context.registerReceiver(screenOnReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))

        onDispose {
            context.unregisterReceiver(screenOffReceiver)
            context.unregisterReceiver(screenOnReceiver)
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
                animeDetail?.title,
                isFavorite.value,
                mainState.isLandscape,
                mainState.networkStatus,
                selectedContentIndex,
                { selectedContentIndex = it },
                episodeDetailComplement,
                { onBackPress() },
                {
                    isFavorite.value = it.isFavorite
                    viewModel.updateEpisodeDetailComplement(it)
                }
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                viewModel.handleSelectedEpisodeServer(episodeSourcesQuery, isRefresh = true)
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
            val videoSize = if (mainState.isLandscape) Modifier.fillMaxSize()
            else if (!isPipMode && !isFullscreen) Modifier.height(250.dp)
            else Modifier.fillMaxSize()

            Column(modifier = Modifier.fillMaxSize()) {
                val videoPlayerModifier = Modifier
                    .then(if (mainState.isLandscape) Modifier.weight(0.5f) else Modifier.fillMaxWidth())
                    .then(videoSize)
                AnimeWatchContent(
                    animeDetail,
                    isFavorite.value,
                    { episodeDetailComplement, seekPosition ->
                        viewModel.updateLastEpisodeWatchedIdAnimeDetailComplement(
                            episodeDetailComplement.id
                        )
                        val updatedEpisodeDetailComplement = episodeDetailComplement.copy(
                            isFavorite = isFavorite.value,
                            lastTimestamp = seekPosition,
                            lastWatched = LocalDateTime.now()
                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        )
                        viewModel.updateEpisodeDetailComplement(updatedEpisodeDetailComplement)
                    },
                    { viewModel.getCachedEpisodeDetailComplement(it) },
                    animeDetailComplement?.episodes,
                    episodeDetailComplement,
                    episodeSourcesQuery,
                    mainState.isConnected,
                    mainState.isLandscape,
                    isPipMode,
                    isFullscreen,
                    scrollState,
                    isScreenOn,
                    onEnterPipMode,
                    { isFullscreen = it },
                    { errorMessage = it },
                    { viewModel.handleSelectedEpisodeServer(it) },
                    selectedContentIndex,
                    videoPlayerModifier,
                    videoSize
                )
            }
        }
    }
}