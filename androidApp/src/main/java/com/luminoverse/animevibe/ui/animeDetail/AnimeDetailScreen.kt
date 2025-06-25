package com.luminoverse.animevibe.ui.animeDetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.ui.animeDetail.components.AnimeDetailTopBar
import com.luminoverse.animevibe.ui.animeDetail.components.LoadingContent
import com.luminoverse.animevibe.ui.animeDetail.components.SuccessContent
import com.luminoverse.animevibe.ui.common.SomethingWentWrongDisplay
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.utils.resource.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailScreen(
    id: Int,
    navController: NavHostController,
    mainState: MainState,
    detailState: DetailState,
    episodeFilterState: EpisodeFilterState,
    onAction: (DetailAction) -> Unit
) {
    val density = LocalDensity.current
    val statusBarPadding = with(density) {
        WindowInsets.systemBars.getTop(density).toDp()
    }
    val navigationBarBottomPadding = with(density) {
        WindowInsets.systemBars.getBottom(density).toDp()
    }
    val navigationBarLeftPadding = with(density) {
        WindowInsets.systemBars.getLeft(
            density,
            if (mainState.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        ).toDp()
    }
    val navigationBarRightPadding = with(density) {
        WindowInsets.systemBars.getRight(
            density,
            if (mainState.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        ).toDp()
    }

    val context = LocalContext.current
    val portraitScrollState = rememberLazyListState()
    val landscapeScrollState = rememberLazyListState()

    val currentAnimeIdState = rememberSaveable { mutableIntStateOf(id) }
    val currentAnimeId = currentAnimeIdState.intValue

    LaunchedEffect(mainState.isConnected) {
        if (!mainState.isConnected) return@LaunchedEffect
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
            .padding(top = statusBarPadding)
    ) {
        AnimeDetailTopBar(
            animeDetail = detailState.animeDetail,
            animeDetailComplement = detailState.animeDetailComplement,
            navController = navController,
            isRtl = mainState.isRtl,
            isLandscape = mainState.isLandscape,
            navigationBarLeftPadding = navigationBarLeftPadding,
            navigationBarRightPadding = navigationBarRightPadding,
            onFavoriteToggle = { onAction(DetailAction.ToggleFavorite(it)) }
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            when (detailState.animeDetail) {
                is Resource.Loading -> LoadingContent(
                    isLandscape = mainState.isLandscape,
                    navigationBarBottomPadding = navigationBarBottomPadding,
                    portraitScrollState = portraitScrollState,
                    landscapeScrollState = landscapeScrollState,
                )

                is Resource.Success -> SuccessContent(
                    detailState = detailState,
                    episodeFilterState = episodeFilterState,
                    navController = navController,
                    context = context,
                    isLandscape = mainState.isLandscape,
                    isConnected = mainState.isConnected,
                    navigationBarBottomPadding = navigationBarBottomPadding,
                    portraitScrollState = portraitScrollState,
                    landscapeScrollState = landscapeScrollState,
                    onAction = onAction,
                    onAnimeIdChange = { newAnimeId ->
                        currentAnimeIdState.intValue = newAnimeId
                    }
                )

                is Resource.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SomethingWentWrongDisplay(
                        message = if (mainState.isConnected) detailState.animeDetail.message else "No internet connection",
                        suggestion = if (mainState.isConnected) null else "Please check your internet connection and try again"
                    )
                }
            }
        }
    }
}