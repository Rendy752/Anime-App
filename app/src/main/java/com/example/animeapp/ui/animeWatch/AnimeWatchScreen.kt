package com.example.animeapp.ui.animeWatch

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.animeapp.ui.main.MainActivity
import com.example.animeapp.ui.main.MainState
import com.example.animeapp.utils.FullscreenUtils
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ScreenOffReceiver
import com.example.animeapp.utils.ScreenOnReceiver
import com.example.animeapp.utils.PipUtil.buildPipActions
import com.example.animeapp.ui.animeWatch.components.AnimeWatchContent
import com.example.animeapp.ui.animeWatch.components.AnimeWatchTopBar
import com.example.animeapp.utils.HlsPlayerUtil
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeWatchScreen(
    malId: Int = 0,
    episodeId: String = "",
    navController: NavHostController,
    mainState: MainState,
    isPipMode: Boolean,
    onEnterPipMode: () -> Unit
) {
    val activity = LocalActivity.current as ViewModelStoreOwner
    val viewModel: AnimeWatchViewModel = hiltViewModel(activity)

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

    // PiP Handling
    val activityAsActivity = LocalActivity.current as MainActivity
    val playerState by HlsPlayerUtil.state.collectAsStateWithLifecycle()

    val onBackPress: () -> Unit = {
        if (isFullscreen) {
            (context as? FragmentActivity)?.let { activity ->
                activity.window?.let { window ->
                    FullscreenUtils.handleFullscreenToggle(
                        window = window,
                        isFullscreen = isFullscreen,
                        isLandscape = mainState.isLandscape,
                        activity = activity,
                        onFullscreenChange = { isFullscreen = it }
                    )
                }
            }
        } else {
            navController.popBackStack()
        }
    }

    BackHandler {
        onBackPress()
    }

    LaunchedEffect(isPipMode, playerState.isPlaying) {
        if (isPipMode) {
            val actions = buildPipActions(activityAsActivity, playerState.isPlaying)
            activityAsActivity.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setActions(actions)
                    .build()
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activityAsActivity.setPictureInPictureParams(
                PictureInPictureParams.Builder().build()
            )
        }
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
        }
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
            }
        ) {
            val videoSize = if (mainState.isLandscape) Modifier.fillMaxSize()
            else if (!isPipMode && !isFullscreen) Modifier.height(250.dp)
            else Modifier.fillMaxSize()

            Column(modifier = Modifier.fillMaxSize()) {
                val videoPlayerModifier = Modifier
                    .then(if (mainState.isLandscape) Modifier.weight(0.5f) else Modifier.fillMaxWidth())
                    .then(videoSize)
                AnimeWatchContent(
                    animeDetail = animeDetail,
                    isFavorite = isFavorite.value,
                    updateStoredWatchState = { episodeDetailComplement, seekPosition ->
                        viewModel.updateLastEpisodeWatchedIdAnimeDetailComplement(
                            episodeDetailComplement.id
                        )
                        val updatedEpisodeDetailComplement = episodeDetailComplement.copy(
                            isFavorite = isFavorite.value,
                            lastTimestamp = seekPosition,
                            lastWatched = LocalDateTime.now()
                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                        viewModel.updateEpisodeDetailComplement(updatedEpisodeDetailComplement)
                    },
                    getCachedEpisodeDetailComplement = viewModel::getCachedEpisodeDetailComplement,
                    episodes = animeDetailComplement?.episodes,
                    episodeDetailComplement = episodeDetailComplement,
                    episodeSourcesQuery = episodeSourcesQuery,
                    isConnected = mainState.isConnected,
                    isLandscape = mainState.isLandscape,
                    isPipMode = isPipMode,
                    isFullscreen = isFullscreen,
                    scrollState = scrollState,
                    isScreenOn = isScreenOn,
                    onEnterPipMode = onEnterPipMode,
                    onFullscreenChange = { isFullscreen = it },
                    onPlayerError = { errorMessage = it },
                    handleSelectedEpisodeServer = viewModel::handleSelectedEpisodeServer,
                    selectedContentIndex = selectedContentIndex,
                    modifier = videoPlayerModifier,
                    videoSize = videoSize
                )
            }
        }
    }
}