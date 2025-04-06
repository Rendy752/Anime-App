package com.example.animeapp.ui.animeHome

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.animeapp.ui.animeHome.components.ContinueWatchingCard
import com.example.animeapp.utils.Resource

@Composable
fun AnimeHomeScreen(currentRoute: String?, navController: NavHostController) {
    val viewModel: HomeViewModel = hiltViewModel()
    val continueWatchingState by viewModel.continueWatchingState.collectAsState()
    var isShowPopup by remember { mutableStateOf(false) }
    var episodeData by remember {
        mutableStateOf<com.example.animeapp.models.EpisodeDetailComplement?>(
            null
        )
    }

    LaunchedEffect(currentRoute) {
        viewModel.fetchContinueWatchingEpisode()
    }

    LaunchedEffect(continueWatchingState) {
        if (continueWatchingState is Resource.Success) {
            episodeData = continueWatchingState.data
            isShowPopup = episodeData != null
            if (isShowPopup) {
                Handler(Looper.getMainLooper()).postDelayed({
                    isShowPopup = false
                }, 10000)
            }
        } else {
            isShowPopup = false
        }
    }

    ContinueWatchingCard(
        isShowPopup = isShowPopup,
        episode = episodeData ?: return,
        onDismiss = { isShowPopup = false })
}