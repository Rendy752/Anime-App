package com.luminoverse.animevibe.ui.animeDetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.ui.animeDetail.components.AnimeDetailTopBar
import com.luminoverse.animevibe.ui.animeDetail.components.LoadingContent
import com.luminoverse.animevibe.ui.animeDetail.components.SuccessContent
import com.luminoverse.animevibe.ui.common.MessageDisplay
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
    val context = LocalContext.current
    val portraitScrollState = rememberLazyListState()
    val landscapeScrollState = rememberLazyListState()

    val currentAnimeIdState = rememberSaveable { mutableIntStateOf(id) }
    val currentAnimeId = currentAnimeIdState.intValue

    LaunchedEffect(mainState.isConnected) {
        if (mainState.isConnected && detailState.animeDetail is Resource.Error) {
            onAction(DetailAction.LoadAnimeDetail(currentAnimeId))
        }
    }

    LaunchedEffect(currentAnimeId) {
        onAction(DetailAction.LoadAnimeDetail(currentAnimeId))
        portraitScrollState.animateScrollToItem(0)
        landscapeScrollState.animateScrollToItem(0)
    }

    Scaffold(topBar = {
        AnimeDetailTopBar(
            animeDetail = detailState.animeDetail,
            animeDetailComplement = detailState.animeDetailComplement,
            navController = navController,
            onFavoriteToggle = { onAction(DetailAction.ToggleFavorite(it)) }
        )
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
        ) {
            when (detailState.animeDetail) {
                is Resource.Loading -> LoadingContent(
                    isLandscape = mainState.isLandscape,
                    portraitScrollState = portraitScrollState,
                    landscapeScrollState = landscapeScrollState,
                )

                is Resource.Success -> SuccessContent(
                    animeDetailData = detailState.animeDetail.data.data,
                    detailState = detailState,
                    episodeFilterState = episodeFilterState,
                    navController = navController,
                    context = context,
                    isLandscape = mainState.isLandscape,
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
                ) { MessageDisplay(message = detailState.animeDetail.message) }
            }
        }
    }
}