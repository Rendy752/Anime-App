package com.example.animeapp.ui.home

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.animeapp.ui.common_ui.ContinueWatchingCard
import com.example.animeapp.utils.Resource

@Composable
fun HomeScreen(navController: NavHostController) {
    val viewModel: HomeViewModel = hiltViewModel()
    val continueWatchingState by viewModel.continueWatchingState.collectAsState()
    var showPopup by remember { mutableStateOf(false) }
    var episodeData by remember {
        mutableStateOf<com.example.animeapp.models.EpisodeDetailComplement?>(
            null
        )
    }

    LaunchedEffect(continueWatchingState) {
        if (continueWatchingState is Resource.Success) {
            continueWatchingState.data?.let {
                episodeData = it
                showPopup = true
                Handler(Looper.getMainLooper()).postDelayed({
                    showPopup = false
                }, 10000)
            }
        } else {
            showPopup = false
        }
    }

    if (showPopup) {
        episodeData?.let {
            Popup(
                alignment = Alignment.BottomEnd,
                offset = IntOffset((-20).dp.value.toInt(), (-20).dp.value.toInt()),
                onDismissRequest = { showPopup = false }
            ) {
                ContinueWatchingCard(episode = it)
            }
        }
    }
}