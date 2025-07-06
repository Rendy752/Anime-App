package com.luminoverse.animevibe.ui.animeDetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.ui.animeDetail.components.AnimeDetailTopBar
import com.luminoverse.animevibe.ui.animeDetail.components.LoadingContent
import com.luminoverse.animevibe.ui.animeDetail.components.SuccessContent
import com.luminoverse.animevibe.ui.common.SharedImageState
import com.luminoverse.animevibe.ui.common.SomethingWentWrongDisplay
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.ui.main.SnackbarMessage
import com.luminoverse.animevibe.utils.onEnableNotifications
import com.luminoverse.animevibe.utils.resource.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailScreen(
    id: Int,
    navController: NavHostController,
    playEpisode: (Int, String) -> Unit,
    rememberedTopPadding: Dp,
    mainState: MainState,
    checkNotificationPermission: () -> Unit,
    setPostNotificationsPermission: (Boolean) -> Unit,
    showSnackbar: (SnackbarMessage) -> Unit,
    dismissSnackbar: () -> Unit,
    showImagePreview: (SharedImageState) -> Unit,
    detailState: DetailState,
    snackbarFlow: Flow<SnackbarMessage>,
    episodeFilterState: EpisodeFilterState,
    onAction: (DetailAction) -> Unit
) {
    val context = LocalContext.current
    val portraitScrollState = rememberLazyListState()
    val landscapeScrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val currentAnimeIdState = rememberSaveable { mutableIntStateOf(id) }
    val currentAnimeId = currentAnimeIdState.intValue

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> checkNotificationPermission() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        setPostNotificationsPermission(isGranted)
        if (isGranted) dismissSnackbar()
    }

    LaunchedEffect(Unit) {
        scope.launch {
            snackbarFlow.collectLatest { snackbarMessage ->
                showSnackbar(snackbarMessage)
            }
        }
    }

    LaunchedEffect(mainState.networkStatus.isConnected) {
        if (!mainState.networkStatus.isConnected) return@LaunchedEffect
        if (detailState.animeDetail is Resource.Error) {
            onAction(DetailAction.LoadAnimeDetail(currentAnimeId))
        }
        if (detailState.animeDetailComplement is Resource.Error && detailState.animeDetail is Resource.Success) {
            onAction(DetailAction.LoadAllEpisode(true))
        }
    }

    LaunchedEffect(currentAnimeId) {
        onAction(DetailAction.LoadAnimeDetail(currentAnimeId))
        portraitScrollState.animateScrollToItem(0)
        landscapeScrollState.animateScrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = rememberedTopPadding)
    ) {
        AnimeDetailTopBar(
            animeDetail = detailState.animeDetail,
            animeDetailComplement = detailState.animeDetailComplement,
            navController = navController,
            playEpisode = playEpisode,
            onFavoriteToggle = { onAction(DetailAction.ToggleFavorite(it)) },
            isPostNotificationsPermissionGranted = mainState.isPostNotificationsPermissionGranted,
            showSnackbar = showSnackbar,
            onEnableNotificationClick = {
                onEnableNotifications(
                    context = context,
                    onPermissionGranted = { setPostNotificationsPermission(true) },
                    permissionLauncher = permissionLauncher,
                    settingsLauncher = settingsLauncher
                )
            }
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceContainer,
            thickness = 2.dp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            when (detailState.animeDetail) {
                is Resource.Loading -> LoadingContent(
                    isLandscape = mainState.isLandscape,
                    portraitScrollState = portraitScrollState,
                    landscapeScrollState = landscapeScrollState,
                )

                is Resource.Success -> SuccessContent(
                    detailState = detailState,
                    episodeFilterState = episodeFilterState,
                    navController = navController,
                    playEpisode = playEpisode,
                    context = context,
                    isLandscape = mainState.isLandscape,
                    portraitScrollState = portraitScrollState,
                    landscapeScrollState = landscapeScrollState,
                    onAction = onAction,
                    onAnimeIdChange = { newAnimeId ->
                        currentAnimeIdState.intValue = newAnimeId
                    },
                    showImagePreview = showImagePreview
                )

                is Resource.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SomethingWentWrongDisplay(
                        message = if (mainState.networkStatus.isConnected) detailState.animeDetail.message else "No internet connection",
                        suggestion = if (mainState.networkStatus.isConnected) null else "Please check your internet connection and try again"
                    )
                }
            }
        }
    }
}